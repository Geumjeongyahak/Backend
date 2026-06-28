# gcloud 기반 GCP 인프라 구성 가이드

Terraform 없이 `gcloud`와 쉘 스크립트로 dev/prod 인프라를 만드는 절차다. 이 방식은 상태 파일을 두지 않으므로, 생성된 리소스 이름과 IP를 문서화하고 동일 스크립트를 반복 실행해도 크게 깨지지 않게 구성한다.

## 구성 목표

```text
App GCE
- e2-small
- 10GB boot disk
- static external IP
- Java 21 runtime
- Spring Boot jar + systemd
- node-exporter systemd service

DB GCE
- e2-micro
- 10GB boot disk
- static internal IP
- apt PostgreSQL + systemd
- node-exporter systemd service
- postgres-exporter systemd service

GCS
- one bucket per environment
- app service account has objectAdmin
- optional public read

Home server
- Prometheus / Alertmanager / Grafana
- Tailscale VPN으로 App/DB exporter를 scrape
- GCE firewall은 Tailscale `41641/udp`만 public 허용
```

## 비용 최소화 원칙

이 구성은 처음부터 managed 서비스를 많이 붙이지 않는 최소 비용 구성을 기준으로 한다.

- PostgreSQL은 Cloud SQL이 아니라 `e2-micro` GCE에 apt 패키지로 수동 설치한 PostgreSQL로 시작한다.
- 로드밸런서는 만들지 않는다. 초기에는 앱 GCE의 고정 IP와 `:8080` 또는 VM 내부 reverse proxy로 처리한다.
- Cloud NAT는 만들지 않는다. DB VM은 외부 IP 없이 두고, 필요한 SSH는 IAP 터널을 쓴다.
- 앱 로그는 VM 디스크에 `application.yyyy-MM-dd.log` 일자별 파일로 저장하고, Logback이 30일 이후 파일을 삭제한다. 파일에는 `WARN`/`ERROR` 이상만 남기므로 Cloud Ops Agent는 JSON 파싱 없이 파일을 Cloud Logging으로 전달한다.
- Cloud Logging 로그 기반 metric/alert는 Google Cloud Monitoring으로 구성한다. 홈서버는 Prometheus/Grafana 중심으로 시스템/API metric을 본다.
- 시스템 메트릭은 `node-exporter`, DB 메트릭은 `postgres-exporter`만 사용하고, Prometheus/Alertmanager/Grafana는 홈서버에서 돌린다.
- Cloud VPN은 사용하지 않고 Tailscale로 홈서버, App VM, DB VM을 같은 tailnet에 붙인다.
- monitoring endpoint는 public exporter 포트가 아니라 Tailscale IP 또는 MagicDNS hostname으로만 접근한다.
- GCE firewall/security group에는 Tailscale direct path용 `41641/udp`만 public 허용한다.
- boot disk는 기본 `10GB pd-standard`로 시작한다. jar와 PostgreSQL 데이터 디렉터리가 커지면 DB부터 증설한다.
- static external IP는 앱 VM에만 둔다. 단, 고정 IP가 VM에 붙어 있지 않으면 비용이 발생할 수 있으니 방치하지 않는다.
- dev 비용을 더 줄이고 싶으면 `SKIP_DB_INSTANCE=true`로 DB VM을 만들지 않고 앱 VM 하나에서 수동 PostgreSQL 구성을 테스트한다.

OAuth client는 `gcloud`만으로 안정적으로 자동 생성하기 어렵다. 스크립트가 redirect URI와 origin을 출력하므로, Google Cloud Console에서 OAuth consent screen과 OAuth client를 수동으로 만든 뒤 앱 서버 `.env`에 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`을 넣는다.

## 1. 준비

```bash
gcloud auth login
gcloud auth application-default login
gcloud config set project <PROJECT_ID>
```

권장:

- dev/prod는 가능하면 GCP 프로젝트를 분리한다.
- 한 프로젝트에서 나눌 경우 리소스 이름은 `gjlearn-dev-*`, `gjlearn-prod-*`처럼 prefix를 분리한다.
- prod의 `SSH_SOURCE_RANGES`는 `0.0.0.0/0`로 두지 않는다. 운영 배포 SSH와 metrics scrape는 Tailscale을 사용한다.

## 2. 환경 파일 작성

```bash
cp scripts/gcp/00_env/dev.env.example scripts/gcp/00_env/dev.env
cp scripts/gcp/00_env/prod.env.example scripts/gcp/00_env/prod.env
```

최소 수정 항목:

- `PROJECT_ID`
- `DEPLOY_OS_USER`
- `SSH_SOURCE_RANGES`
- `API_DOMAIN`
- `FRONTEND_ORIGIN`
- `FRONTEND_REDIRECT_URI`
- `STORAGE_BUCKET_NAME`
- `STORAGE_FOLDERS`: 재생성 시 `.keep` placeholder를 만들 GCS prefix 목록. 기본 권장값은 `profiles,editor,site-contents,documents/attachments,documents/purchase-items`.
- `HTTP_PORTS`: 기본은 `tcp:8080`; VM 내부 reverse proxy를 붙인 뒤 `tcp:80,tcp:443`로 바꾼다.
- `ALERT_NOTIFICATION_CHANNELS`: Cloud Monitoring notification channel을 이미 만들었다면 comma-separated resource name으로 넣는다. 예: `projects/<PROJECT_ID>/notificationChannels/<CHANNEL_ID>`

GCS bucket을 삭제 후 재생성한 경우에는 bucket 생성만으로 충분하지 않다. `scripts/gcp/01_infra/01_provision-gcp.sh`는 다음 항목을 함께 보정한다.

- app service account에 bucket-level `roles/storage.objectAdmin` 부여
- signed URL 생성을 위한 project-level `roles/iam.serviceAccountTokenCreator` 부여
- `STORAGE_PUBLIC_READ=true`일 때 `allUsers` `roles/storage.objectViewer` 부여. 현재 단일 GCS bucket은 이미지 public URL을 바로 반환하므로 기본값은 `true`다.
- `STORAGE_FOLDERS`에 정의된 prefix에 `.keep` 객체 생성

GCS IAM과 앱 서버용 service account key JSON/base64 env snippet만 별도로 만들 때는 다음 스크립트를 사용한다. 기본 출력 위치는 gitignore된 `scripts/gcp/00_env/`이며, 생성된 key JSON과 env snippet은 secret이므로 git에 넣지 않는다.

```bash
scripts/gcp/02_credentials/00_create-gcs-credentials.sh scripts/gcp/00_env/dev.env

# 기존 app.env까지 자동 갱신하려면
APP_ENV_PATH=scripts/gcp/00_env/dev.app.env \
  scripts/gcp/02_credentials/00_create-gcs-credentials.sh scripts/gcp/00_env/dev.env

# 키 JSON 경로를 지정하려면
scripts/gcp/02_credentials/00_create-gcs-credentials.sh scripts/gcp/00_env/dev.env scripts/gcp/00_env/gjlearn-dev-gcs-sa-key.json
```

생성물:

- service account: `${APP_SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com`
- key JSON: `scripts/gcp/00_env/gjlearn-gcs-<env>-sa-key.json` 또는 지정 경로
- env snippet: `scripts/gcp/00_env/gjlearn-gcs-<env>-credentials.env`
- `GCP_ENCODED_CREDENTIALS`: key JSON을 한 줄 base64로 인코딩한 값

prod 예시:

```env
ENVIRONMENT=prod
PROJECT_ID=gjlearn-prod
APP_INSTANCE_NAME=gjlearn-prod-app
DB_INSTANCE_NAME=gjlearn-prod-db
APP_MACHINE_TYPE=e2-small
DB_MACHINE_TYPE=e2-micro
BOOT_DISK_SIZE_GB=10
BOOT_DISK_TYPE=pd-standard
SSH_SOURCE_RANGES=203.0.113.10/32
API_DOMAIN=api.example.com
FRONTEND_ORIGIN=https://app.example.com
```

dev를 단일 VM으로 더 싸게 가져갈 때:

```env
SKIP_DB_INSTANCE=true
APP_MACHINE_TYPE=e2-small
BOOT_DISK_SIZE_GB=10
```

이 경우 별도 DB VM과 DB internal IP는 만들지 않는다. 앱 VM 안에 PostgreSQL을 수동 설치하거나 로컬 Docker Compose 구성으로만 테스트한다.

## 3. 인프라 생성

요청한 prod 기본 리소스명으로 빠르게 초기 인프라만 만들 때는 다음 스크립트를 사용한다.

```bash
PROJECT_ID=geumgeong-yahack scripts/gcp/01_infra/00_create-initial-infra.sh
```

기본 생성값:

| 리소스 | 기본값 |
|---|---|
| App static external IP | `gjlearn-api-ip` |
| App VM | `gjlearn-api-server-1`, `e2-small`, boot disk `10GB`, Ubuntu 24.04 |
| DB VM | `gjlearn-postgres-db-1`, `e2-micro`, boot disk `20GB`, Ubuntu 24.04, external IP 없음 |
| GCS bucket | `gs://geumjeong-public-prod`, `asia-northeast3` |

스크립트는 Tailscale용 `41641/udp`, App HTTP `8080`, IAP SSH, App-to-DB PostgreSQL 방화벽도 함께 만든다. 일반 public SSH는 기본으로 만들지 않는다. 필요할 때만 `CREATE_PUBLIC_SSH_RULE=true SSH_SOURCE_RANGES=<your-ip>/32`를 지정한다.

실행 전 생성 명령만 확인하려면:

```bash
PROJECT_ID=geumgeong-yahack DRY_RUN=true scripts/gcp/01_infra/00_create-initial-infra.sh
```

env 파일 값으로 이름/리전/태그를 override하려면:

```bash
scripts/gcp/01_infra/00_create-initial-infra.sh scripts/gcp/00_env/prod.env
```

기존 범용 provision 스크립트를 사용할 수도 있다.

dev:

```bash
scripts/gcp/01_infra/01_provision-gcp.sh scripts/gcp/00_env/dev.env
```

prod:

```bash
scripts/gcp/01_infra/01_provision-gcp.sh scripts/gcp/00_env/prod.env
```

생성되는 것:

- Compute API, IAM API, Storage API 활성화
- 앱 service account
- GCS bucket
- 앱 service account의 GCS object admin 권한
- 앱 static external IP
- DB static internal IP, `SKIP_DB_INSTANCE=true`면 생략
- app/db GCE 인스턴스
- firewall
  - app HTTP/HTTPS/8080
  - Tailscale UDP 41641 for app/db
  - metrics ports are reached through Tailscale, not public firewall
  - app to db PostgreSQL
  - SSH / IAP SSH
- Java/PostgreSQL client/node-exporter 설치 startup script
- `~/app-dev`, `~/app-dev/logs/app`, `~/db-dev` 디렉터리

출력만 다시 보고 싶으면:

```bash
scripts/gcp/01_infra/02_print-outputs.sh scripts/gcp/00_env/prod.env
```

## 4. 서버 .env 생성

앱 서버용:

```bash
scripts/gcp/03_env_render/00_render-server-env.sh scripts/gcp/00_env/prod.env app > scripts/gcp/00_env/prod.app.env
chmod 600 scripts/gcp/00_env/prod.app.env
```

DB 서버용:

```bash
scripts/gcp/03_env_render/00_render-server-env.sh scripts/gcp/00_env/prod.env db > scripts/gcp/00_env/prod.db.env
chmod 600 scripts/gcp/00_env/prod.db.env
```

생성된 파일에서 반드시 바꿀 값:

- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `JWE_SECRET`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `FIREBASE_*`: Firebase 사용 시 service-account base64와 web config 값


`ADMIN_PASSWORD`는 최초 관리자 계정 생성 후 앱 서버 `.env`에서 제거해도 된다.

## 5. 파일 복사

앱 서버:

```bash
export ENV_FILE=scripts/gcp/00_env/prod.env
source "$ENV_FILE"

gcloud compute scp build/libs/*.jar scripts/gcp/05_app/01_install-app-service.sh \
  "${APP_INSTANCE_NAME}:~/app-dev/" \
  --project "$PROJECT_ID" \
  --zone "$ZONE"

gcloud compute scp scripts/gcp/00_env/prod.app.env \
  "${APP_INSTANCE_NAME}:~/app-dev/.env" \
  --project "$PROJECT_ID" \
  --zone "$ZONE"
```

DB 서버는 외부 IP가 없으면 IAP 터널을 사용한다.

```bash
gcloud compute scp scripts/gcp/04_db/01_install-db-service.sh \
  "${DB_INSTANCE_NAME}:~/db-dev/" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap

gcloud compute scp scripts/gcp/00_env/prod.db.env \
  "${DB_INSTANCE_NAME}:~/db-dev/.env" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap
```

## 6. 배포

DB 먼저:

```bash
gcloud compute ssh "$DB_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap \
  --command "cd ~/db-dev && chmod +x 01_install-db-service.sh && ./01_install-db-service.sh"
```

앱:

```bash
gcloud compute ssh "$APP_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --command "cd ~/app-dev && mv *.jar app.jar && chmod +x 01_install-app-service.sh && ./01_install-app-service.sh"
```

앱 로그:

```bash
gcloud compute ssh "$APP_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --command "sudo journalctl -u gjlearn-app -n 200 --no-pager"
```

파일 로그는 앱 서버의 `~/app-dev/logs/app/application.yyyy-MM-dd.log`에 일자별로 남는다. Logback이 `LOG_FILE_LEVEL` 이상을 기록하고 `LOG_FILE_MAX_HISTORY=30` 기준으로 오래된 파일을 삭제한다. Cloud Ops Agent는 JSON 파싱 없이 `LOG_UPLOAD_PATH`에 매칭되는 파일을 Cloud Logging log id `gjlearn-<env>-app`으로 전달한다.

Cloud Logging 로그 기반 metric과 alert policy를 생성/갱신한다. 알림 수신 채널을 이미 만든 경우 `scripts/gcp/00_env/<env>.env`에 `ALERT_NOTIFICATION_CHANNELS`를 넣고 실행한다.

```bash
scripts/gcp/06_observability/00_configure-cloud-alerts.sh scripts/gcp/00_env/dev.env
```

Cloud Logging 확인:

```bash
gcloud logging read \
  'resource.type="gce_instance" AND log_id("gjlearn-dev-app")' \
  --project "$PROJECT_ID" \
  --limit 20 \
  --format json
```

## 7. 홈서버 모니터링과 Cloud Logging 알림 최소 구성

비용 최소화를 최우선으로 한다. GCP에는 Prometheus, Grafana, Alertmanager, Loki를 올리지 않는다. 홈서버는 메트릭 대시보드/장애 감지를 담당하고, 애플리케이션 `WARN`/`ERROR` 로그 알림은 Cloud Logging 로그 기반 metric과 Cloud Monitoring alert policy로 처리한다.

### 7.1 기본 구조

```text
Home server
- Prometheus
- Alertmanager
- Grafana
- optional Loki, 로그 대시보드가 필요해진 뒤 추가

  ↕ Tailscale 100.64.0.0/10 또는 MagicDNS

App GCE
- app jar :8080
- actuator :9090
- node-exporter :9100
- google-cloud-ops-agent, app WARN/ERROR logs -> Cloud Logging
- tailscaled

DB GCE
- PostgreSQL :5432, App VM에서만 접근
- node-exporter :9100
- postgres-exporter :9187
- tailscaled
```

`8080`, `9100`, `9187`, `5432`는 public internet에 직접 열지 않는다. GCE firewall에는 Tailscale direct path용 `41641/udp`만 public 허용한다. SSH는 최초 구성/비상 접근용으로 관리자 IP 또는 IAP만 허용한다.

금정야학 기본 운영은 단순 Tailscale 노드 모드다. App/DB/Home server가 각각 tailnet에 직접 붙으므로 GCE VM의 IP forwarding, Linux sysctl forwarding, Tailscale `--advertise-routes`는 사용하지 않는다.

Tailscale 인증은 각 VM에서 최초 1회 수행한다.

```bash
# App VM 예시
sudo tailscale up \
  --advertise-tags=tag:gjlearn-prod-app,tag:gjlearn-app,tag:prod \
  --accept-dns=false

# DB VM 예시
sudo tailscale up \
  --advertise-tags=tag:gjlearn-prod-db,tag:gjlearn-db,tag:prod \
  --accept-dns=false

tailscale ip -4
tailscale status
```

MagicDNS를 켜면 IP 대신 `gjlearn-app.<tailnet>.ts.net` 같은 hostname을 Prometheus와 GitHub Actions에서 사용할 수 있다.

### 7.2 홈서버 Prometheus target 파일

Backend repo의 `infra/monitoring`은 편집/독립 실행용 mirror이고, 홈서버 운영 경로는 `/home/min/Infra/monitoring`이다. dev target은 아래 파일에 있고, prod는 실제 prod tailnet 노드가 생긴 뒤 같은 형식으로 `infra/monitoring/prometheus/targets/gjlearn/prod/`에 추가한 뒤 동기화한다.

```text
infra/monitoring/prometheus/targets/gjlearn/dev/app-actuator.yml
infra/monitoring/prometheus/targets/gjlearn/dev/node-exporter.yml
infra/monitoring/prometheus/targets/gjlearn/dev/postgres-exporter.yml
```

IP로 관리하려면 target에 `tailscale ip -4` 결과인 `100.x.x.x:port`를 넣는다.

자세한 절차는 `docs/deployment/tailscale-observability-deploy.md`를 따른다.

### 7.3 최소 Prometheus 알림 룰

홈서버 Prometheus/Alertmanager에는 장애 감지용 알림을 둔다. 앱 `WARN`/`ERROR` 로그 알림은 Cloud Logging 기반 알림을 사용한다.

```yaml
groups:
  - name: gjlearn-basic
    rules:
      - alert: AppActuatorDown
        expr: up{project="gjlearn", role="app-actuator"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "API actuator is down"

      - alert: AppNodeDown
        expr: up{project="gjlearn", role="node-exporter", service="app"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "App VM node-exporter is down"

      - alert: DbNodeDown
        expr: up{project="gjlearn", role="node-exporter", service="db"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "DB VM node-exporter is down"

      - alert: PostgresDown
        expr: pg_up{project="gjlearn", role="postgres-exporter"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "PostgreSQL is down"

      - alert: DiskAlmostFull
        expr: 100 * (1 - node_filesystem_avail_bytes{project="gjlearn", role="node-exporter", fstype!~"tmpfs|overlay"} / node_filesystem_size_bytes{project="gjlearn", role="node-exporter", fstype!~"tmpfs|overlay"}) > 85
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Disk usage above 85%"
```

### 7.4 Alertmanager 알림 채널

비용 최소 알림 채널은 Telegram 또는 Discord webhook을 우선 사용한다. 알림 판단과 발송은 모두 홈서버에서 수행한다.

Discord 예시:

```yaml
route:
  receiver: discord
  group_by: ['alertname', 'instance']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 3h

receivers:
  - name: discord
    discord_configs:
      - webhook_url: 'https://discord.com/api/webhooks/...'
        send_resolved: true
```

Telegram 예시:

```yaml
route:
  receiver: telegram
  group_by: ['alertname', 'instance']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 3h

receivers:
  - name: telegram
    telegram_configs:
      - bot_token: '<BOT_TOKEN>'
        chat_id: 123456789
        send_resolved: true
```

## 8. OAuth 설정

OAuth client는 두 용도로 분리한다.

- 로그인/회원가입용: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`
- Google Drive 업로드용: `GOOGLE_DRIVE_OAUTH_CLIENT_ID`, `GOOGLE_DRIVE_OAUTH_CLIENT_SECRET`, `GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN`

### 8.1 로그인/회원가입용 Google OAuth

Google Cloud Console에서:

1. APIs & Services > OAuth consent screen 구성
2. APIs & Services > Credentials > Create Credentials > OAuth client ID
3. Application type: Web application
4. Authorized JavaScript origins:
   - `FRONTEND_ORIGIN`
5. Authorized redirect URIs:
   - `https://API_DOMAIN/api/v1/auth/google/callback`
   - 도메인이 아직 없으면 `http://APP_EXTERNAL_IP:8080/api/v1/auth/google/callback`
6. 발급된 client id/secret을 앱 서버 `.env`의 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`에 입력

이 client는 서버가 `openid email profile` scope로 Google 로그인/회원가입을 처리할 때만 사용한다.

### 8.2 Google Drive 업로드용 OAuth

Workspace Shared Drive 없이 개인 Google Drive를 쓰는 경우 service account JSON으로는 저장 quota 문제를 피할 수 없다. Drive 소유자 Google 계정으로 OAuth refresh token을 1회 발급해 앱 서버에 저장한다.

Drive 업로드용 OAuth client:

1. APIs & Services > Credentials > Create Credentials > OAuth client ID
2. Application type: Web application
3. Authorized redirect URIs:
   - `https://developers.google.com/oauthplayground`
4. Authorized JavaScript origins는 비워도 된다.

refresh token 발급:

1. `https://developers.google.com/oauthplayground` 접속
2. 우측 설정에서 `Use your own OAuth credentials` 체크
3. Drive 업로드용 client id/secret 입력
4. scope에 `https://www.googleapis.com/auth/drive` 입력
5. Drive 소유자 Google 계정으로 승인
6. `Exchange authorization code for tokens` 실행
7. 나온 `refresh_token`을 앱 서버 `.env`에 입력

앱 서버 `.env`:

```env
GOOGLE_DRIVE_OAUTH_CLIENT_ID=<Drive OAuth client id>
GOOGLE_DRIVE_OAUTH_CLIENT_SECRET=<Drive OAuth client secret>
GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN=<Drive OAuth refresh token>
GOOGLE_DRIVE_FOLDER_ID_BOARD=<Drive folder id>
GOOGLE_DRIVE_FOLDER_ID_HANDOVER=<Drive folder id>
GOOGLE_DRIVE_FOLDER_ID_EXAM_MATERIALS=<Drive folder id>
GOOGLE_DRIVE_FOLDER_ID_DOCUMENT_FORMS=<Drive folder id>
GOOGLE_DRIVE_FOLDER_ID_MEETING_RECORDS=<Drive folder id>
```

OAuth consent screen이 `Testing` 상태면 refresh token이 7일 만료될 수 있다. 지속 운영은 `Production` 상태로 전환한다.

출력 확인:

```bash
scripts/gcp/01_infra/02_print-outputs.sh scripts/gcp/00_env/prod.env
```

## 9. 운영 팁

- 비용 우선순위는 “VM 수 줄이기 > 디스크 증설 미루기 > managed service 미루기” 순서다.
- prod에서 DB 분리를 유지해야 하면 현재처럼 `e2-small app + e2-micro db`가 최소선이다.
- dev는 필요할 때만 켜고, 사용하지 않을 때는 VM을 중지한다. 중지해도 boot disk와 static IP 비용은 남는다.
- DB GCE는 외부 IP 없이 두고 IAP SSH를 사용한다.
- DB `5432`는 앱 network tag에서만 허용한다.
- `8080`, `9100`, `9187`, `5432`는 public에 직접 열지 않는다. 홈서버와 GitHub Actions는 Tailscale IP/MagicDNS로 접근한다.
- MagicDNS를 켜면 재시작 후 Tailscale IP가 바뀌어도 Prometheus와 Actions 설정을 hostname 기준으로 유지할 수 있다.
- prod에서는 `FLYWAY_BASELINE_ON_MIGRATE=false`가 기본이다.
- 기존 수동 DB를 Flyway로 편입할 때만 `FLYWAY_BASELINE_ON_MIGRATE=true`를 일회성으로 켠다.
- 앱 service account를 VM에 붙였으므로 가능하면 `GCP_ENCODED_CREDENTIALS` 없이 metadata credentials를 사용한다. 현재 앱 설정이 encoded key를 요구하면, 추후 optional 처리로 바꾸는 것이 좋다.
- dev와 prod의 bucket, DB, app instance는 절대 공유하지 않는다.
- GCS bucket 이름은 전 세계에서 unique해야 하므로 프로젝트 id prefix를 붙이는 편이 안전하다.
- 10GB boot disk는 최소 구성이다. jar/log와 PostgreSQL 데이터가 빠르게 커지면 먼저 로그/백업 정리를 하고, 그래도 부족하면 DB 서버부터 20GB 이상으로 늘린다.

## 10. 정리 명령

수동 삭제가 필요할 때만 실행한다. DB 디스크 삭제는 데이터 삭제와 같다.

```bash
gcloud compute instances delete "$APP_INSTANCE_NAME" --project "$PROJECT_ID" --zone "$ZONE"
gcloud compute instances delete "$DB_INSTANCE_NAME" --project "$PROJECT_ID" --zone "$ZONE"
gcloud compute addresses delete "$APP_STATIC_IP_NAME" --project "$PROJECT_ID" --region "$REGION"
gcloud compute addresses delete "$DB_INTERNAL_IP_NAME" --project "$PROJECT_ID" --region "$REGION"
gcloud storage rm --recursive "gs://${STORAGE_BUCKET_NAME}"
```
