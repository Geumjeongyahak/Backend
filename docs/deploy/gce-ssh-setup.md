# GCE 배포 및 SSH 설정 가이드

이 문서는 Docker/GHCR 기반 배포가 아니라, GitHub Actions가 Spring Boot jar를 빌드해 App GCE에 복사하고 systemd 서비스를 재시작하는 구성을 기준으로 한다.

## 1. GCE 접속 정보 확인

배포 대상은 App GCE의 외부 IP와 SSH 사용자다.

```bash
gcloud compute instances list

gcloud compute ssh <app-instance-name> \
  --zone <gce-zone> \
  --command="whoami"
```

GitHub Secrets에 넣을 값:

| 이름 | 설명 | 확인 방법 |
| :--- | :--- | :--- |
| `DEV_GCE_HOST` / `PROD_GCE_HOST` | App GCE의 SSH host 또는 external IP | `gcloud compute instances list` |
| `DEV_GCE_USER` / `PROD_GCE_USER` | SSH 접속 OS 사용자 | `gcloud compute ssh ... --command="whoami"` |
| `DEV_GCE_SSH_KEY` / `PROD_GCE_SSH_KEY` | GitHub Actions 전용 private key 전문 | `cat gce-deploy-key` |

`GHCR_TOKEN`, `APP_IMAGE`는 사용하지 않는다.

## 2. SSH 키 생성 및 등록

GitHub Actions가 App GCE에 비밀번호 없이 접속하려면 전용 SSH 키 쌍이 필요하다. passphrase는 비워둔다.

```bash
ssh-keygen -t rsa -b 4096 -f ./gce-deploy-key -C "github-actions-deploy"
```

GCE 인스턴스 메타데이터에 public key를 등록한다.

```bash
gcloud compute instances add-metadata <app-instance-name> \
  --zone <gce-zone> \
  --metadata-from-file ssh-keys=<(echo "<GCE_USER>:$(cat gce-deploy-key.pub)")
```

GitHub repository의 `Settings > Secrets and variables > Actions > Repository secrets`에 다음을 등록한다.

```text
DEV_GCE_HOST
DEV_GCE_USER
DEV_GCE_SSH_KEY
PROD_GCE_HOST
PROD_GCE_USER
PROD_GCE_SSH_KEY
```

## 3. 서버 `.env`와 GitHub Secrets의 경계

GitHub Actions는 `.env`를 생성하거나 덮어쓰지 않는다. 애플리케이션 환경 변수는 App GCE 인스턴스의 `~/app-dev/.env`에 직접 둔다.

앱 서버 예시:

```env
SPRING_PROFILES_ACTIVE=prod
APP_PORT=8080
MANAGEMENT_PORT=8080
NODE_EXPORTER_PORT=9100
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=WARN
APP_LOG_DIR=./logs/app
LOG_FILE_PATTERN=./logs/app/application.%d{yyyy-MM-dd}.log
LOG_UPLOAD_PATH=./logs/app/application.*.log
LOG_FILE_MAX_HISTORY=30

POSTGRES_HOST=DB_SERVER_PRIVATE_IP
POSTGRES_PORT=5432
POSTGRES_DB=geumjeongyahak
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
POSTGRES_OPTIONS=
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false

ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-this-strong-password-1A!
ADMIN_NAME=관리자

JWT_SECRET=change-me-at-least-256-bits
JWE_SECRET=change-me-at-least-256-bits
CORS_ALLOWED_ORIGINS=https://app.example.com

GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=https://api.example.com/api/v1/auth/google/callback
FRONTEND_REDIRECT_URI=https://app.example.com/auth/google/callback

GCP_PROJECT_ID=your-project-id
GCP_PROD_BUCKET_NAME=your-bucket
GCP_DEV_BUCKET_NAME=your-bucket
GCP_ENCODED_CREDENTIALS=
```

`ADMIN_PASSWORD`는 최초 관리자 계정 생성 후 제거해도 된다.

DB 서버 예시:

```env
DB_PORT=5432
DB_LISTEN_ADDRESS=*
APP_DB_CIDR=APP_SERVER_PRIVATE_IP/32
NODE_EXPORTER_PORT=9100
POSTGRES_EXPORTER_PORT=9187

POSTGRES_DB=geumjeongyahak
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
```

DB 서버의 `.env`는 `~/db-dev/.env`에 둔다.

## 4. 최초 수동 구성

인프라 생성:

```bash
scripts/gcp/01_infra/01_provision-gcp.sh scripts/gcp/00_env/prod.env
```

서버 env 생성:

```bash
scripts/gcp/03_env_render/00_render-server-env.sh scripts/gcp/00_env/prod.env app > scripts/gcp/00_env/prod.app.env
scripts/gcp/03_env_render/00_render-server-env.sh scripts/gcp/00_env/prod.env db > scripts/gcp/00_env/prod.db.env
chmod 600 scripts/gcp/00_env/prod.app.env scripts/gcp/00_env/prod.db.env
```

비밀번호와 secret을 수정한 뒤 복사한다.

```bash
gcloud compute scp scripts/gcp/00_env/prod.app.env \
  "$APP_INSTANCE_NAME:~/app-dev/.env" \
  --project "$PROJECT_ID" \
  --zone "$ZONE"

gcloud compute scp scripts/gcp/04_db/01_install-db-service.sh scripts/gcp/00_env/prod.db.env \
  "$DB_INSTANCE_NAME:~/db-dev/" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap

gcloud compute ssh "$DB_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap \
  --command "cd ~/db-dev && mv prod.db.env .env && chmod +x 01_install-db-service.sh && ./01_install-db-service.sh"
```

## 5. GitHub Actions 배포 흐름

`.github/workflows/deploy-dev.yml`은 다음만 수행한다.

1. JDK 21 설정
2. `./gradlew bootJar -x test`
3. SSH key 기반 SCP로 `build/libs/*.jar`와 `scripts/gcp/05_app/01_install-app-service.sh`를 App GCE `~/app-dev/`로 복사
4. `~/app-dev/app.jar`로 교체
5. `scripts/gcp/05_app/01_install-app-service.sh`로 `gjlearn-app.service` 재시작

App GCE에는 `~/app-dev/.env`와 배포 public key가 미리 준비되어 있어야 한다.

## 6. PR 머지 전 수동 배포 검증

Docker 이미지 push/pull 대신 현재 브랜치의 jar를 직접 빌드해 App GCE에서 실행한다.

```bash
./gradlew bootJar -x test

scp build/libs/*.jar scripts/gcp/05_app/01_install-app-service.sh \
  "$GCE_USER@$GCE_HOST:~/app-dev/"

ssh "$GCE_USER@$GCE_HOST" \
  "cd ~/app-dev && mv *.jar app.jar && chmod +x 01_install-app-service.sh && ./01_install-app-service.sh"
```

확인:

```bash
gcloud compute ssh "$APP_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --command "sudo systemctl status gjlearn-app --no-pager && sudo journalctl -u gjlearn-app -n 100 --no-pager"
```

API smoke test:

```bash
curl -fsS "http://<APP_EXTERNAL_IP>:8080/actuator/health"
```

## 7. 모니터링

비용 최소 구성을 유지하기 위해 Prometheus, Alertmanager, Grafana는 GCE가 아니라 홈서버에서 운영한다.

| 포트 | 대상 | 설명 |
| :--- | :--- | :--- |
| `8080` | App GCE Spring Actuator | `/actuator/prometheus` |
| `9100` | App GCE / DB GCE node-exporter | CPU, memory, disk, network metrics |
| `9187` | DB GCE postgres-exporter | PostgreSQL metrics |

권장 구조:

```text
Home Prometheus
  -> gjlearn-app.<tailnet>.ts.net:8080/actuator/prometheus
  -> gjlearn-app.<tailnet>.ts.net:9100/metrics
  -> gjlearn-db.<tailnet>.ts.net:9100/metrics
  -> gjlearn-db.<tailnet>.ts.net:9187/metrics
```

`8080`, `9100`, `9187`, `5432`는 public internet에 직접 열지 않는다. Tailscale direct path용 `41641/udp`만 public 허용한다.

## 8. 문제 해결

### SSH 접속 실패

1. GitHub Secrets의 `DEV_GCE_HOST`/`PROD_GCE_HOST`, `*_GCE_USER`, `*_GCE_SSH_KEY` 값을 확인한다.
2. GCE 인스턴스에 SSH 사용자와 매칭되는 public key가 등록되어 있는지 확인한다.
3. GCP firewall에서 GitHub Actions runner 또는 self-hosted runner source IP의 `22/tcp` 접근이 허용되어 있는지 확인한다.

### 앱 서비스 실패

```bash
sudo systemctl status gjlearn-app --no-pager
sudo journalctl -u gjlearn-app -n 200 --no-pager
```

자주 보는 원인:

- `~/app-dev/.env` 누락
- `POSTGRES_HOST`/`POSTGRES_PASSWORD` 불일치
- DB 방화벽에서 App VM 접근 차단
- `JWT_SECRET`, `JWE_SECRET`, GCP/OAuth 환경 변수 누락

### DB 서비스 실패

```bash
sudo systemctl status postgresql --no-pager
sudo journalctl -u postgresql -n 200 --no-pager
sudo systemctl status prometheus-postgres-exporter --no-pager
```

`APP_DB_CIDR`는 가능하면 앱 서버 private IP `/32`로 좁힌다.
