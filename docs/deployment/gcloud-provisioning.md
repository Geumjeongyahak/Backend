# gcloud 기반 GCP 인프라 구성 가이드

Terraform 없이 `gcloud`와 쉘 스크립트로 dev/prod 인프라를 만드는 절차다. 이 방식은 상태 파일을 두지 않으므로, 생성된 리소스 이름과 IP를 문서화하고 동일 스크립트를 반복 실행해도 크게 깨지지 않게 구성한다.

## 구성 목표

```text
App GCE
- e2-small
- 10GB boot disk
- static external IP
- Docker / Compose
- app + node-exporter

DB GCE
- e2-micro
- 10GB boot disk
- static internal IP
- Docker / Compose
- PostgreSQL + node-exporter

GCS
- one bucket per environment
- app service account has objectAdmin
- optional public read

Home server
- Prometheus / Alertmanager / Grafana
- 비용 최소 기본안은 VPN/Cloud NAT 없이 App VM monitoring gateway를 scrape
- 홈서버 DDNS가 가리키는 현재 공인 IP만 GCE firewall에서 허용
```

## 비용 최소화 원칙

이 구성은 처음부터 managed 서비스를 많이 붙이지 않는 최소 비용 구성을 기준으로 한다.

- PostgreSQL은 Cloud SQL이 아니라 `e2-micro` GCE의 Docker PostgreSQL로 시작한다.
- 로드밸런서는 만들지 않는다. 초기에는 앱 GCE의 고정 IP와 `:8080` 또는 VM 내부 reverse proxy로 처리한다.
- Cloud NAT는 만들지 않는다. DB VM은 외부 IP 없이 두고, 필요한 SSH는 IAP 터널을 쓴다.
- Cloud Logging/Loki/Alloy는 붙이지 않는다. 앱 로그는 VM 디스크에 30일 rolling file로 저장한다.
- 시스템 메트릭은 `node-exporter`만 사용하고, Prometheus/Alertmanager/Grafana는 홈서버에서 돌린다.
- Cloud VPN은 기본 구성에서 사용하지 않는다. 홈서버가 GCE를 scrape해야 하면 App VM의 Caddy/Nginx monitoring gateway를 통해 내부 exporter에 접근한다.
- 홈서버는 DDNS로 현재 공인 IP를 관리하고, 홈서버 cron이 DDNS를 resolve해서 GCE firewall source range를 `/32`로 갱신한다.
- monitoring endpoint는 public exporter 포트가 아니라 App VM의 단일 HTTPS 포트(예: `8443`)만 열고, 방화벽 IP 제한과 Basic Auth를 함께 사용한다.
- boot disk는 기본 `10GB pd-standard`로 시작한다. Docker image와 DB volume이 커지면 DB부터 증설한다.
- static external IP는 앱 VM에만 둔다. 단, 고정 IP가 VM에 붙어 있지 않으면 비용이 발생할 수 있으니 방치하지 않는다.
- dev 비용을 더 줄이고 싶으면 `SKIP_DB_INSTANCE=true`로 DB VM을 만들지 않고 앱 VM 하나에서 bundled compose로 테스트한다.

OAuth client는 `gcloud`만으로 안정적으로 자동 생성하기 어렵다. 스크립트가 redirect URI와 origin을 출력하므로, Google Cloud Console에서 OAuth consent screen과 OAuth client를 수동으로 만든 뒤 앱 서버 `.env`에 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`을 넣는다.

## 1. 준비

```bash
gcloud auth login
gcloud auth application-default login
gcloud config set project <PROJECT_ID>
```

권장:

- dev/prod는 가능하면 GCP 프로젝트를 분리한다.
- 한 프로젝트에서 나눌 경우 리소스 이름은 `sonmoum-dev-*`, `sonmoum-prod-*`처럼 prefix를 분리한다.
- prod의 `SSH_SOURCE_RANGES`, `MONITORING_SOURCE_RANGES`는 `0.0.0.0/0`로 두지 않는다.

## 2. 환경 파일 작성

```bash
cp scripts/gcp/env/dev.env.example scripts/gcp/env/dev.env
cp scripts/gcp/env/prod.env.example scripts/gcp/env/prod.env
```

최소 수정 항목:

- `PROJECT_ID`
- `DEPLOY_OS_USER`
- `SSH_SOURCE_RANGES`
- `MONITORING_SOURCE_RANGES`
- `API_DOMAIN`
- `FRONTEND_ORIGIN`
- `FRONTEND_REDIRECT_URI`
- `STORAGE_BUCKET_NAME`
- `HTTP_PORTS`: 기본은 `tcp:8080`; VM 내부 reverse proxy를 붙인 뒤 `tcp:80,tcp:443`로 바꾼다.

prod 예시:

```env
ENVIRONMENT=prod
PROJECT_ID=sonmoum-prod
APP_INSTANCE_NAME=sonmoum-prod-app
DB_INSTANCE_NAME=sonmoum-prod-db
APP_MACHINE_TYPE=e2-small
DB_MACHINE_TYPE=e2-micro
BOOT_DISK_SIZE_GB=10
BOOT_DISK_TYPE=pd-standard
SSH_SOURCE_RANGES=203.0.113.10/32
# 홈서버 DDNS를 resolve한 현재 공인 IP /32. DDNS 갱신 스크립트로 자동 갱신 가능.
MONITORING_SOURCE_RANGES=203.0.113.20/32
API_DOMAIN=api.example.com
FRONTEND_ORIGIN=https://app.example.com
```

dev를 단일 VM으로 더 싸게 가져갈 때:

```env
SKIP_DB_INSTANCE=true
APP_MACHINE_TYPE=e2-small
BOOT_DISK_SIZE_GB=10
```

이 경우 별도 DB VM과 DB internal IP는 만들지 않는다. 앱 VM 안에서 `docker-compose.yml` 기반 bundled 구성을 수동으로 올려 테스트한다.

## 3. 인프라 생성

dev:

```bash
scripts/gcp/provision-gcp.sh scripts/gcp/env/dev.env
```

prod:

```bash
scripts/gcp/provision-gcp.sh scripts/gcp/env/prod.env
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
  - app monitoring gateway 또는 Actuator/node-exporter from monitoring source
  - db node-exporter from app gateway 또는 monitoring source
  - app to db PostgreSQL
  - SSH / IAP SSH
- Docker 설치 startup script
- `~/app-dev`, `~/app-dev/logs/app`, `~/db-dev` 디렉터리

출력만 다시 보고 싶으면:

```bash
scripts/gcp/print-outputs.sh scripts/gcp/env/prod.env
```

## 4. 서버 .env 생성

앱 서버용:

```bash
scripts/gcp/render-server-env.sh scripts/gcp/env/prod.env app > /tmp/app.env
```

DB 서버용:

```bash
scripts/gcp/render-server-env.sh scripts/gcp/env/prod.env db > /tmp/db.env
```

생성된 파일에서 반드시 바꿀 값:

- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `JWE_SECRET`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- 필요 시 `APP_IMAGE`

`ADMIN_PASSWORD`는 최초 관리자 계정 생성 후 앱 서버 `.env`에서 제거해도 된다.

## 5. 파일 복사

앱 서버:

```bash
export ENV_FILE=scripts/gcp/env/prod.env
source "$ENV_FILE"

gcloud compute scp docker-compose.app.yml docker-compose.dev.yml Makefile \
  "${APP_INSTANCE_NAME}:~/app-dev/" \
  --project "$PROJECT_ID" \
  --zone "$ZONE"

gcloud compute scp /tmp/app.env \
  "${APP_INSTANCE_NAME}:~/app-dev/.env" \
  --project "$PROJECT_ID" \
  --zone "$ZONE"
```

DB 서버는 외부 IP가 없으면 IAP 터널을 사용한다.

```bash
gcloud compute scp docker-compose.db.yml Makefile \
  "${DB_INSTANCE_NAME}:~/db-dev/" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap

gcloud compute scp /tmp/db.env \
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
  --command "cd ~/db-dev && make deploy-db-dev && make ps-db-dev"
```

앱:

```bash
gcloud compute ssh "$APP_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --command "cd ~/app-dev && APP_IMAGE=ghcr.io/geumjeongyahak/backend:dev-latest make deploy-app-dev && make ps-app-dev"
```

앱 로그:

```bash
gcloud compute ssh "$APP_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --command "cd ~/app-dev && make logs-app-dev"
```

파일 로그는 앱 서버의 `~/app-dev/logs/app`에 30일 rolling으로 남는다.

## 7. 홈서버 모니터링/알림 최소 구성

비용 최소화를 최우선으로 한다. GCP에는 Prometheus, Grafana, Alertmanager, Loki를 올리지 않는다. 홈서버가 모든 대시보드와 알림 판단을 담당하고, GCE에는 exporter와 단순 reverse proxy만 둔다.

### 7.1 기본 구조

```text
Home server
- Prometheus
- Alertmanager
- Grafana
- optional Loki, 로그 대시보드가 필요해진 뒤 추가

App GCE
- app :8080
- actuator :9090
- node-exporter :9100
- Caddy/Nginx monitoring gateway :8443

DB GCE
- PostgreSQL :5432, App VM에서만 접근
- node-exporter :9100, App VM gateway에서만 접근
- optional postgres-exporter :9187, 운영 안정화 단계에서 추가
```

Prometheus는 홈서버에서 App VM의 monitoring gateway만 scrape한다. DB VM은 외부 IP와 Cloud NAT 없이 유지한다.

```text
Home Prometheus
  -> https://API_DOMAIN:8443/app
  -> https://API_DOMAIN:8443/app-node
  -> https://API_DOMAIN:8443/db-node
  -> https://API_DOMAIN:8443/postgres, postgres-exporter를 붙인 경우
```

App VM의 gateway는 내부 endpoint로만 proxy한다.

```text
/app      -> 127.0.0.1:9090/actuator/prometheus
/app-node -> 127.0.0.1:9100/metrics
/db-node  -> DB_INTERNAL_IP:9100/metrics
/postgres -> DB_INTERNAL_IP:9187/metrics
```

### 7.2 DDNS 기반 GCE firewall 갱신

GCE firewall은 DDNS hostname을 직접 source range로 받을 수 없다. 반드시 DDNS를 IP로 resolve한 뒤 `<ip>/32`를 넣어야 한다.

홈서버에서 먼저 확인한다.

```bash
curl -4 ifconfig.me
dig +short A <HOME_DDNS_HOST>
```

두 값이 같으면 DDNS가 현재 홈서버 outbound 공인 IP를 제대로 가리키는 것이다. 라우터에서 홈서버에 할당한 `192.168.x.x` 같은 내부 고정 IP는 GCE firewall source range로 사용할 수 없다.

홈서버 cron에서 다음 스크립트를 5~10분마다 실행한다.

```bash
#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="sonmoum-prod"
RULE_NAME="sonmoum-prod-allow-monitoring-gateway"
NETWORK="default"
TARGET_TAG="sonmoum-prod-app"
DDNS_HOST="home.example.ddns.net"
PORTS="tcp:8443"

CURRENT_IP="$(dig +short A "$DDNS_HOST" | tail -n1)"
if [[ -z "$CURRENT_IP" ]]; then
  echo "failed to resolve DDNS host: $DDNS_HOST" >&2
  exit 1
fi

SOURCE_RANGE="${CURRENT_IP}/32"

if gcloud compute firewall-rules describe "$RULE_NAME" --project="$PROJECT_ID" >/dev/null 2>&1; then
  EXISTING="$(gcloud compute firewall-rules describe "$RULE_NAME" \
    --project="$PROJECT_ID" \
    --format='value(sourceRanges.list())')"
  if [[ "$EXISTING" == "$SOURCE_RANGE" ]]; then
    echo "firewall already up to date: $SOURCE_RANGE"
    exit 0
  fi
  gcloud compute firewall-rules update "$RULE_NAME" \
    --project="$PROJECT_ID" \
    --source-ranges="$SOURCE_RANGE"
  echo "updated firewall source range: $SOURCE_RANGE"
else
  gcloud compute firewall-rules create "$RULE_NAME" \
    --project="$PROJECT_ID" \
    --network="$NETWORK" \
    --allow="$PORTS" \
    --source-ranges="$SOURCE_RANGE" \
    --target-tags="$TARGET_TAG"
  echo "created firewall rule: $SOURCE_RANGE"
fi
```

cron 예시:

```cron
*/10 * * * * /opt/sonmoum/update-gce-monitoring-firewall.sh >> /var/log/sonmoum-gce-firewall-ddns.log 2>&1
```

### 7.3 App VM Caddy gateway 예시

API 공개 포트와 monitoring 포트는 분리한다. 예를 들어 `443`은 API 공개용, `8443`은 홈서버 Prometheus 전용으로 둔다.

```caddyfile
api.example.com {
  reverse_proxy 127.0.0.1:8080
}

api.example.com:8443 {
  basicauth {
    prometheus <caddy-hashed-password>
  }

  handle /app {
    rewrite * /actuator/prometheus
    reverse_proxy 127.0.0.1:9090
  }

  handle /app-node {
    rewrite * /metrics
    reverse_proxy 127.0.0.1:9100
  }

  handle /db-node {
    rewrite * /metrics
    reverse_proxy 10.0.0.20:9100
  }

  handle /postgres {
    rewrite * /metrics
    reverse_proxy 10.0.0.20:9187
  }
}
```

`9090`, `9100`, `9187`, `5432`는 public internet에 직접 열지 않는다. DB VM의 `9100`/`9187`은 App VM network tag에서만 접근 가능하게 제한한다.

### 7.4 홈서버 Prometheus scrape 예시

```yaml
global:
  scrape_interval: 60s
  evaluation_interval: 60s

rule_files:
  - /etc/prometheus/rules/*.yml

alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

scrape_configs:
  - job_name: sonmoum-api
    scheme: https
    metrics_path: /app
    basic_auth:
      username: prometheus
      password: <strong-password>
    static_configs:
      - targets:
          - api.example.com:8443

  - job_name: sonmoum-app-node
    scheme: https
    metrics_path: /app-node
    basic_auth:
      username: prometheus
      password: <strong-password>
    static_configs:
      - targets:
          - api.example.com:8443

  - job_name: sonmoum-db-node
    scheme: https
    metrics_path: /db-node
    basic_auth:
      username: prometheus
      password: <strong-password>
    static_configs:
      - targets:
          - api.example.com:8443
```

postgres-exporter를 추가한 뒤에는 다음 job을 더한다.

```yaml
  - job_name: sonmoum-postgres
    scheme: https
    metrics_path: /postgres
    basic_auth:
      username: prometheus
      password: <strong-password>
    static_configs:
      - targets:
          - api.example.com:8443
```

### 7.5 최소 알림 룰

처음에는 장애 감지용 알림만 둔다. 로그 기반 알림은 Loki를 도입한 뒤 추가한다.

```yaml
groups:
  - name: sonmoum-basic
    rules:
      - alert: AppActuatorDown
        expr: up{job="sonmoum-api"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "API actuator is down"

      - alert: AppNodeDown
        expr: up{job="sonmoum-app-node"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "App VM node-exporter is down"

      - alert: DbNodeDown
        expr: up{job="sonmoum-db-node"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "DB VM node-exporter is down"

      - alert: DiskAlmostFull
        expr: 100 - ((node_filesystem_avail_bytes{fstype!~"tmpfs|overlay"} * 100) / node_filesystem_size_bytes{fstype!~"tmpfs|overlay"}) > 85
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Disk usage above 85%"

      - alert: HostHighMemoryUsage
        expr: 100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) > 90
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Memory usage above 90%"

      - alert: HostHighCpuUsage
        expr: 100 * (1 - avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m]))) > 85
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "CPU usage above 85%"
```

postgres-exporter를 붙인 경우:

```yaml
      - alert: PostgresDown
        expr: pg_up == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "PostgreSQL is down"

      - alert: PostgresDeadlocks
        expr: increase(pg_stat_database_deadlocks[5m]) > 0
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL deadlock detected"
```

### 7.6 Alertmanager 알림 채널

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

Google Cloud Console에서:

1. APIs & Services > OAuth consent screen 구성
2. APIs & Services > Credentials > Create Credentials > OAuth client ID
3. Application type: Web application
4. Authorized JavaScript origins:
   - `FRONTEND_ORIGIN`
5. Authorized redirect URIs:
   - `https://API_DOMAIN/api/v1/auth/google/callback`
   - 도메인이 아직 없으면 `http://APP_EXTERNAL_IP:8080/api/v1/auth/google/callback`
6. 발급된 client id/secret을 앱 서버 `.env`에 입력

출력 확인:

```bash
scripts/gcp/print-outputs.sh scripts/gcp/env/prod.env
```

## 9. 운영 팁

- 비용 우선순위는 “VM 수 줄이기 > 디스크 증설 미루기 > managed service 미루기” 순서다.
- prod에서 DB 분리를 유지해야 하면 현재처럼 `e2-small app + e2-micro db`가 최소선이다.
- dev는 필요할 때만 켜고, 사용하지 않을 때는 VM을 중지한다. 중지해도 boot disk와 static IP 비용은 남는다.
- DB GCE는 외부 IP 없이 두고 IAP SSH를 사용한다.
- DB `5432`는 앱 network tag에서만 허용한다.
- `9100` node-exporter와 `9187` postgres-exporter는 public에 직접 열지 않는다. 비용 최소 기본안에서는 App VM monitoring gateway가 내부로 proxy하고, 홈서버는 gateway만 scrape한다.
- 홈서버 DDNS를 쓰는 경우 GCE firewall에는 DDNS hostname이 아니라 현재 resolve된 IP `/32`만 넣는다. IP 변경은 홈서버 cron으로 갱신한다.
- prod에서는 `FLYWAY_BASELINE_ON_MIGRATE=false`가 기본이다.
- 기존 수동 DB를 Flyway로 편입할 때만 `FLYWAY_BASELINE_ON_MIGRATE=true`를 일회성으로 켠다.
- 앱 service account를 VM에 붙였으므로 가능하면 `GCP_ENCODED_CREDENTIALS` 없이 metadata credentials를 사용한다. 현재 앱 설정이 encoded key를 요구하면, 추후 optional 처리로 바꾸는 것이 좋다.
- dev와 prod의 bucket, DB, app instance는 절대 공유하지 않는다.
- GCS bucket 이름은 전 세계에서 unique해야 하므로 프로젝트 id prefix를 붙이는 편이 안전하다.
- 10GB boot disk는 최소 구성이다. Docker image와 DB volume이 빠르게 커지면 먼저 `docker image prune`과 백업 정리를 하고, 그래도 부족하면 DB 서버부터 20GB 이상으로 늘린다.

## 10. 정리 명령

수동 삭제가 필요할 때만 실행한다. DB 디스크 삭제는 데이터 삭제와 같다.

```bash
gcloud compute instances delete "$APP_INSTANCE_NAME" --project "$PROJECT_ID" --zone "$ZONE"
gcloud compute instances delete "$DB_INSTANCE_NAME" --project "$PROJECT_ID" --zone "$ZONE"
gcloud compute addresses delete "$APP_STATIC_IP_NAME" --project "$PROJECT_ID" --region "$REGION"
gcloud compute addresses delete "$DB_INTERNAL_IP_NAME" --project "$PROJECT_ID" --region "$REGION"
gcloud storage rm --recursive "gs://${STORAGE_BUCKET_NAME}"
```
