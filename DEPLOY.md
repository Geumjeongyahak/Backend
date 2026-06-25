# GJLearn GCE 배포 가이드

이 문서는 GCP/GCE dev/prod 배포를 순서대로 실행하기 위한 최상위 runbook입니다. 현재 운영 형태는 App VM + DB VM 분리, Spring Boot jar + systemd, PostgreSQL apt 패키지, Tailscale 단순 노드 모드입니다.

## 스크립트 구조

모든 GCP 배포 스크립트는 `scripts/gcp/` 아래에서 단계 번호별 폴더로 나눕니다.

```text
scripts/gcp/
├── 00_env/
│   ├── dev.env.example              # dev 인프라/env 예시
│   ├── prod.env.example             # prod 인프라/env 예시
│   ├── dev.env / prod.env           # local infra/env, gitignore
│   ├── dev.app.env / prod.app.env   # App VM runtime env, gitignore
│   ├── dev.db.env / prod.db.env     # DB VM runtime env, gitignore
│   └── gjlearn-gcs-*.env/json       # credential snippets/keys, gitignore
├── 01_infra/
│   ├── 00_create-initial-infra.sh    # 초기/레거시 인프라 생성용
│   ├── 01_provision-gcp.sh           # 현재 표준 GCE/GCS/IAM/firewall 생성
│   └── 02_print-outputs.sh           # 생성된 IP/redirect/env 힌트 출력
├── 02_credentials/
│   └── 00_create-gcs-credentials.sh  # GCS IAM/key JSON/base64 env snippet 생성
├── 03_env_render/
│   └── 00_render-server-env.sh       # app/db 서버용 .env 템플릿 렌더링
├── 04_db/
│   ├── 00_startup-db-manual.sh       # DB VM startup bootstrap
│   └── 01_install-db-service.sh      # PostgreSQL/exporter/Tailscale 설치
├── 05_app/
│   ├── 00_startup-app-manual.sh      # App VM startup bootstrap
│   └── 01_install-app-service.sh     # jar systemd/Ops Agent/node-exporter 설치
└── 06_observability/
    └── 00_configure-cloud-alerts.sh  # Cloud Logging WARN/ERROR metric/alert 생성
```

## 0. 로컬 전제

```bash
gcloud auth login
gcloud auth application-default login
gcloud config set project <PROJECT_ID>
```

dev/prod 기준 env 파일을 준비합니다. dev는 `dev` branch merge 시, prod는 `main` branch merge 시 배포됩니다.

```bash
cp scripts/gcp/00_env/dev.env.example scripts/gcp/00_env/dev.env
vi scripts/gcp/00_env/dev.env

cp scripts/gcp/00_env/prod.env.example scripts/gcp/00_env/prod.env
vi scripts/gcp/00_env/prod.env

chmod 600 scripts/gcp/00_env/dev.env scripts/gcp/00_env/prod.env
```

최소 확인 항목:

```env
PROJECT_ID=geumgeong-yahack
REGION=us-east1
ZONE=us-east1-c
APP_INSTANCE_NAME=gjlearn-dev-app
DB_INSTANCE_NAME=gjlearn-dev-db
APP_MACHINE_TYPE=e2-small
DB_MACHINE_TYPE=e2-micro
DEPLOY_OS_USER=ubuntu
SERVER_APP_DIR=/home/ubuntu/app-dev
SERVER_DB_DIR=/home/ubuntu/db-dev
API_DOMAIN=dev.geumjeongschool.com
FRONTEND_ORIGIN=https://dev.geumjeongschool.com
FRONTEND_REDIRECT_URI=https://dev.geumjeongschool.com/auth/google/callback
APP_TAILSCALE_TAGS=tag:gjlearn
DB_TAILSCALE_TAGS=tag:gjlearn
```

## 1. 인프라 생성/보정

```bash
scripts/gcp/01_infra/01_provision-gcp.sh scripts/gcp/00_env/dev.env
```

이 단계가 보장하는 것:

- Compute/IAM/Storage/Logging/Monitoring API 활성화
- App service account 생성
- GCS bucket 생성 및 prefix `.keep` 생성
- bucket `roles/storage.objectAdmin`
- project `roles/iam.serviceAccountTokenCreator`
- project `roles/logging.logWriter`
- App static external IP
- DB internal IP
- firewall rules
- App/DB GCE instance 생성

출력만 다시 보고 싶으면:

```bash
scripts/gcp/01_infra/02_print-outputs.sh scripts/gcp/00_env/dev.env
```

## 2. GCS credential 생성

GCE VM에서는 VM에 연결된 service account를 쓰는 것이 가장 안전합니다. 그래도 앱 설정에서 `GCP_ENCODED_CREDENTIALS`가 필요하면 아래 스크립트로 key JSON과 base64 env snippet을 생성합니다.

```bash
scripts/gcp/02_credentials/00_create-gcs-credentials.sh scripts/gcp/00_env/dev.env
```

생성물은 `/tmp`가 아니라 gitignore된 `scripts/gcp/00_env/` 아래에 둡니다.

- key JSON: `scripts/gcp/00_env/gjlearn-gcs-dev-sa-key.json`
- env snippet: `scripts/gcp/00_env/gjlearn-gcs-dev-credentials.env`

기존 app env까지 자동 갱신하려면:

```bash
APP_ENV_PATH=scripts/gcp/00_env/dev.app.env \
  scripts/gcp/02_credentials/00_create-gcs-credentials.sh scripts/gcp/00_env/dev.env
```

주의: key JSON과 env snippet은 secret입니다. git에 넣지 않습니다.

## 3. 서버 env 생성

이제 서버 runtime env도 `/tmp`가 아니라 gitignore된 `scripts/gcp/00_env/` 아래에 저장합니다. 환경별 파일명은 다음처럼 고정합니다.

| 환경 | infra env | app runtime env | db runtime env |
|------|-----------|-----------------|----------------|
| dev | `scripts/gcp/00_env/dev.env` | `scripts/gcp/00_env/dev.app.env` | `scripts/gcp/00_env/dev.db.env` |
| prod | `scripts/gcp/00_env/prod.env` | `scripts/gcp/00_env/prod.app.env` | `scripts/gcp/00_env/prod.db.env` |

dev env 생성:

```bash
scripts/gcp/03_env_render/00_render-server-env.sh scripts/gcp/00_env/dev.env db > scripts/gcp/00_env/dev.db.env
scripts/gcp/03_env_render/00_render-server-env.sh scripts/gcp/00_env/dev.env app > scripts/gcp/00_env/dev.app.env
chmod 600 scripts/gcp/00_env/dev.db.env scripts/gcp/00_env/dev.app.env
```

prod env 생성:

```bash
scripts/gcp/03_env_render/00_render-server-env.sh scripts/gcp/00_env/prod.env db > scripts/gcp/00_env/prod.db.env
scripts/gcp/03_env_render/00_render-server-env.sh scripts/gcp/00_env/prod.env app > scripts/gcp/00_env/prod.app.env
chmod 600 scripts/gcp/00_env/prod.db.env scripts/gcp/00_env/prod.app.env
```

DB env에서 반드시 설정합니다.

```env
POSTGRES_PASSWORD=<app과 같은 DB 비밀번호>
```

비밀번호/키 생성 예시:

```bash
# DB/Admin password 후보
openssl rand -base64 24

# JWT/JWE secret 후보: 256-bit 이상. dev/prod는 서로 다른 값 사용
openssl rand -base64 48
openssl rand -base64 48
```

App env에서 반드시 설정/확인:

```env
POSTGRES_PASSWORD=<db.env와 같은 값>
ADMIN_EMAIL=<초기 관리자 이메일>
ADMIN_PASSWORD=<초기 관리자 비밀번호>
JWT_SECRET=<openssl 등으로 생성>
JWE_SECRET=<openssl 등으로 생성>
GOOGLE_CLIENT_ID=<Google OAuth client id>
GOOGLE_CLIENT_SECRET=<Google OAuth client secret>
GCP_ENCODED_CREDENTIALS=<필요 시 base64 key json>

# Firebase 사용 시
FIREBASE_ENABLED=true
FIREBASE_PROJECT_ID=<firebase project id>
FIREBASE_CREDENTIALS_BASE64=<firebase service-account json base64>
FIREBASE_WEB_API_KEY=<web config apiKey>
FIREBASE_WEB_AUTH_DOMAIN=<web config authDomain>
FIREBASE_WEB_PROJECT_ID=<web config projectId>
FIREBASE_WEB_STORAGE_BUCKET=<web config storageBucket>
FIREBASE_WEB_MESSAGING_SENDER_ID=<web config messagingSenderId>
FIREBASE_WEB_APP_ID=<web config appId>
FIREBASE_WEB_VAPID_KEY=<web push vapid key>
```

Firebase service-account JSON을 env 값으로 넣을 때는 한 줄 base64로 변환합니다. JSON 원본과 env 파일은 git에 넣지 않습니다.

```bash
base64 -w0 path/to/firebase-service-account.json
```

Google OAuth Console에는 환경별 callback을 등록해야 합니다.

| 환경 | Authorized JavaScript origins | Authorized redirect URI | FRONTEND_REDIRECT_URI |
|------|-------------------------------|--------------------------|-----------------------|
| dev | `https://dev.geumjeongschool.com` | `https://dev.geumjeongschool.com/api/v1/auth/google/callback` | `https://dev.geumjeongschool.com/auth/google/callback` |
| prod | `<운영 프론트 origin>` | `<운영 API origin>/api/v1/auth/google/callback` | `<운영 프론트 origin>/auth/google/callback` |

dev/prod는 Google OAuth client를 분리하는 것을 권장합니다. 같은 client를 공유할 경우 callback/origin을 모두 등록해야 합니다.

## 4. DB VM 설치

환경 로드:

```bash
ENV=dev
ENV_FILE="scripts/gcp/00_env/${ENV}.env"
APP_ENV_FILE="scripts/gcp/00_env/${ENV}.app.env"
DB_ENV_FILE="scripts/gcp/00_env/${ENV}.db.env"
source "$ENV_FILE"
```

DB 설치 스크립트와 env 복사:

```bash
gcloud compute scp scripts/gcp/04_db/01_install-db-service.sh \
  "${DB_INSTANCE_NAME}:~/db-dev/" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap

gcloud compute scp "$DB_ENV_FILE" \
  "${DB_INSTANCE_NAME}:~/db-dev/.env" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap
```

DB 설치 실행:

```bash
gcloud compute ssh "$DB_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap \
  --command "cd ~/db-dev && chmod +x 01_install-db-service.sh && ./01_install-db-service.sh"
```

확인:

```bash
gcloud compute ssh "$DB_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap \
  --command "sudo -u postgres psql -d geumjeongyahak -tAc 'select current_database(), current_user;' && curl -fsS http://localhost:9100/metrics >/dev/null && curl -fsS http://localhost:9187/metrics >/dev/null && echo db-ok"
```

Tailscale 인증이 필요하면:

```bash
gcloud compute ssh "$DB_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap \
  --command "sudo tailscale up --advertise-tags=${DB_TAILSCALE_TAGS:-tag:gjlearn} --accept-dns=false"
```

## 5. App VM 설치

jar 빌드:

```bash
./gradlew bootJar -x test
```

App 설치 스크립트, env, jar 복사:

```bash
gcloud compute scp build/libs/*.jar scripts/gcp/05_app/01_install-app-service.sh \
  "${APP_INSTANCE_NAME}:~/app-dev/" \
  --project "$PROJECT_ID" \
  --zone "$ZONE"

gcloud compute scp "$APP_ENV_FILE" \
  "${APP_INSTANCE_NAME}:~/app-dev/.env" \
  --project "$PROJECT_ID" \
  --zone "$ZONE"
```

App 설치 실행:

```bash
gcloud compute ssh "$APP_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --command "cd ~/app-dev && mv *.jar app.jar && chmod +x 01_install-app-service.sh && ./01_install-app-service.sh"
```

확인:

```bash
gcloud compute ssh "$APP_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --command "curl -fsS http://localhost:8080/actuator/health && sudo systemctl status gjlearn-app --no-pager --full"
```

Tailscale 인증이 필요하면:

```bash
gcloud compute ssh "$APP_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --command "sudo tailscale up --advertise-tags=${APP_TAILSCALE_TAGS:-tag:gjlearn} --accept-dns=false"
```

## 6. Cloud Logging WARN/ERROR 알림

App VM은 `~/app-dev/logs/app/application.yyyy-MM-dd.log` 형식으로 일자별 파일 로그를 남깁니다. Logback 파일 appender는 `WARN`/`ERROR` 이상만 기록하고 `LOG_FILE_MAX_HISTORY=30` 기준으로 30일 이후 파일을 삭제합니다. Cloud Ops Agent는 JSON 파싱 없이 `LOG_UPLOAD_PATH`에 매칭되는 파일을 Cloud Logging으로 전달합니다.

알림 정책 생성/갱신:

```bash
scripts/gcp/06_observability/00_configure-cloud-alerts.sh scripts/gcp/00_env/dev.env
```

알림을 실제로 받으려면 Cloud Monitoring notification channel을 만든 뒤 `scripts/gcp/00_env/dev.env`에 넣고 다시 실행합니다.

```env
ALERT_NOTIFICATION_CHANNELS=projects/geumgeong-yahack/notificationChannels/<CHANNEL_ID>
```

Cloud Logging 확인:

```bash
gcloud logging read \
  'resource.type="gce_instance" AND log_id("gjlearn-dev-app")' \
  --project "$PROJECT_ID" \
  --limit 20 \
  --format json
```

## 7. 홈서버 Prometheus 확인

Prometheus scrape target은 Tailscale IP 또는 MagicDNS hostname을 사용합니다.

- App actuator: `gjlearn-dev-app.<tailnet>.ts.net:8080/actuator/prometheus`
- App node exporter: `gjlearn-dev-app.<tailnet>.ts.net:9100`
- DB node exporter: `gjlearn-dev-db.<tailnet>.ts.net:9100`
- DB postgres exporter: `gjlearn-dev-db.<tailnet>.ts.net:9187`

`infra/monitoring/prometheus.yml`을 실제 target으로 수정한 뒤 홈서버에서:

```bash
make deploy-monitoring
```

## 8. 임시 Cloud NAT 정리

DB/App VM에 외부 IP가 없어서 apt 설치를 위해 임시 Cloud NAT를 만들었다면, 모든 설치가 끝난 뒤 삭제할 수 있습니다.

```bash
gcloud compute routers nats delete gjlearn-dev-temp-nat \
  --router gjlearn-dev-temp-nat-router \
  --router-region us-east1 \
  --project geumgeong-yahack

gcloud compute routers delete gjlearn-dev-temp-nat-router \
  --region us-east1 \
  --project geumgeong-yahack
```

삭제 전 App/DB 설치와 Tailscale 인증이 끝났는지 확인하세요.

## 9. GitHub Actions 배포

branch merge 기준으로 dev/prod 배포를 분리합니다.

| workflow | trigger | 대상 |
|----------|---------|------|
| `.github/workflows/deploy-dev.yml` | `dev` branch push/merge, manual dispatch | dev App VM |
| `.github/workflows/deploy-prod.yml` | `main` branch push/merge, manual dispatch | prod App VM |

GitHub Actions는 Tailscale runner 연결 없이 SSH key로 App VM에 직접 접속합니다. 필수 GitHub Secrets:

```text
DEV_GCE_HOST       # 없으면 기존 GCE_HOST fallback
DEV_GCE_USER       # 없으면 기존 GCE_USER fallback
DEV_GCE_SSH_KEY    # 없으면 기존 GCE_SSH_KEY fallback
PROD_GCE_HOST
PROD_GCE_USER
PROD_GCE_SSH_KEY
```

GitHub CLI로 설정하는 예시:

```bash
gh secret set DEV_GCE_HOST --body '<dev-app-external-ip-or-hostname>'
gh secret set DEV_GCE_USER --body '<dev-ssh-user>'
gh secret set DEV_GCE_SSH_KEY < path/to/dev-deploy-key

gh secret set PROD_GCE_HOST --body '<prod-app-external-ip-or-hostname>'
gh secret set PROD_GCE_USER --body '<prod-ssh-user>'
gh secret set PROD_GCE_SSH_KEY < path/to/prod-deploy-key
```

서버에는 각 공개키를 배포 사용자 `~/.ssh/authorized_keys`에 등록합니다. private key는 GitHub Secret에만 넣고 repository 파일이나 `scripts/gcp/00_env/`에 저장하지 않습니다.

prod workflow는 의도치 않은 dev host 배포를 막기 위해 prod SSH/host secret에는 fallback을 두지 않습니다. prod App VM의 `~/app-dev/.env`는 workflow가 덮어쓰지 않으므로, 최초 수동 배포 단계에서 prod runtime env를 먼저 배치해야 합니다.
