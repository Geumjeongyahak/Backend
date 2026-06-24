# GCE 배포 및 SSH 설정 가이드

이 문서는 `gcloud` CLI를 사용하여 Google Compute Engine(GCE)의 접속 정보를 확인하고, GitHub Actions 배포를 위한 SSH 키 설정 및 시크릿 관리 방법을 설명합니다.

---

## 1. GCE 접속 정보 확인 (gcloud)

GCP 프로젝트가 설정된 로컬 환경에서 아래 명령어를 사용하여 정보를 확인합니다.

### 1.1 인스턴스 목록 및 IP(Host) 확인
배포 대상 인스턴스의 `EXTERNAL_IP`가 GitHub Secrets의 **`GCE_HOST`**가 됩니다.
```bash
gcloud compute instances list
```

### 1.2 현재 SSH 사용자(User) 확인
GCE에 접속할 때 사용하는 기본 사용자 이름은 보통 로컬 시스템의 계정명 또는 GCP 계정 이메일의 앞부분입니다. GitHub Secrets의 **`GCE_USER`**에 해당합니다.
```bash
# SSH 접속을 시도하여 OS 상의 사용자 이름을 확인합니다.
gcloud compute ssh [INSTANCE_NAME] --command="whoami"
```

---

## 2. SSH 키 생성 및 인스턴스 등록

GitHub Actions가 GCE에 비밀번호 없이 접속하려면 전용 SSH 키 쌍이 필요합니다.

### 2.1 SSH 키 쌍 생성
로컬에서 배포 전용 키를 생성합니다. (비밀번호/Passphrase는 비워둡니다.)
```bash
ssh-keygen -t rsa -b 4096 -f ./gce-deploy-key -C "github-actions-deploy"
```
*   `gce-deploy-key`: Private Key (내용을 복사하여 GitHub **`GCE_SSH_KEY`**에 등록)
*   `gce-deploy-key.pub`: Public Key (GCE 인스턴스에 등록)

### 2.2 Public Key를 GCE 인스턴스에 추가
생성한 공개키(`.pub`)를 GCE 메타데이터에 등록하여 접속을 허용합니다.

**방법 A: gcloud 명령어로 추가 (추천)**
```bash
# 기존 메타데이터에 공개키 추가
gcloud compute instances add-metadata [INSTANCE_NAME] \
    --metadata-from-file ssh-keys=<(echo "[GCE_USER]:$(cat gce-deploy-key.pub)")
```

**방법 B: GCP 콘솔에서 추가**
1.  [GCE 인스턴스 상세 페이지]로 이동
2.  [수정(Edit)] 클릭
3.  [SSH 키] 항목에서 [항목 추가] 클릭
4.  `gce-deploy-key.pub` 파일의 내용 전체를 붙여넣기 후 저장

---

## 3. GitHub Secrets 설정 가이드

GitHub 리포지토리의 **Settings > Secrets and variables > Actions**에 아래 항목들을 정확히 등록합니다.

| 이름 | 내용 설명 | 확인 방법 |
| :--- | :--- | :--- |
| **`GCE_HOST`** | 앱 GCE 인스턴스의 외부 IP | `gcloud compute instances list` |
| **`GCE_USER`** | SSH 접속 계정명 | `gcloud compute ssh ... --command="whoami"` |
| **`GCE_SSH_KEY`** | 생성한 Private Key 전문 | `cat gce-deploy-key` (내용 전체) |
| **`GHCR_TOKEN`** | GitHub Personal Access Token | [GitHub PAT 설정](https://github.com/settings/tokens) (Classic 권한: `read:packages`, `write:packages`) |

`GCE_ENV_DEV`는 사용하지 않습니다. 애플리케이션 환경 변수는 앱 GCE 인스턴스의 `~/app-dev/.env` 파일로 직접 관리합니다.

### 3.1 GitHub Actions Secrets 등록

GitHub repository에서 아래 경로로 이동합니다.

```text
Settings > Secrets and variables > Actions > Repository secrets
```

`New repository secret`으로 아래 값을 등록합니다.

#### `GCE_HOST`

앱 GCE 인스턴스의 고정 외부 IP를 등록합니다. 현재 App GCE가 static external IP로 연결되어 있다면 그 IP를 그대로 사용합니다.

확인:

```bash
gcloud compute instances list
```

또는 특정 인스턴스만 확인:

```bash
gcloud compute instances describe <app-instance-name> \
  --zone <gce-zone> \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
```

등록 예시:

```text
GCE_HOST=34.xxx.xxx.xxx
```

#### `GCE_USER`

GitHub Actions가 SSH로 접속할 GCE OS 사용자 이름입니다.

확인:

```bash
gcloud compute ssh <app-instance-name> \
  --zone <gce-zone> \
  --command="whoami"
```

등록 예시:

```text
GCE_USER=min
```

#### `GCE_SSH_KEY`

GitHub Actions 전용 SSH private key 전문을 등록합니다.

키 생성 예시:

```bash
ssh-keygen -t rsa -b 4096 -f ./gce-deploy-key -C "github-actions-deploy"
```

등록 값:

```bash
cat gce-deploy-key
```

`-----BEGIN OPENSSH PRIVATE KEY-----`부터 `-----END OPENSSH PRIVATE KEY-----`까지 줄바꿈 포함 전체를 secret 값으로 넣습니다.

GCE에는 public key를 등록합니다.

```bash
gcloud compute instances add-metadata <app-instance-name> \
  --zone <gce-zone> \
  --metadata-from-file ssh-keys=<(echo "<GCE_USER>:$(cat gce-deploy-key.pub)")
```

#### `GHCR_TOKEN`

이미 생성한 GitHub classic token을 등록합니다.

필요 권한:

```text
read:packages
write:packages
```

현재 workflow에서는 두 곳에서 사용됩니다.

- GitHub Actions가 backend image를 GHCR에 push할 때는 기본 `secrets.GITHUB_TOKEN` 사용
- GCE 서버가 GHCR에서 private image를 pull할 때는 `secrets.GHCR_TOKEN` 사용

GCE 서버에서 pull만 한다면 `read:packages`가 핵심입니다. 수동 push 검증까지 같은 token으로 처리하려면 `write:packages`도 같이 둡니다.

### 3.2 GitHub Actions Variables 선택 사항

현재 workflow는 아래 값을 파일에 고정하지 않고 workflow `env`와 repository 정보로 계산합니다.

```yaml
REGISTRY: ghcr.io
IMAGE_NAME: ${GITHUB_REPOSITORY,,}
```

따라서 Actions Variables에 별도로 등록할 값은 없습니다. 바꾸고 싶다면 아래 경로에서 repository variable을 추가하고 workflow를 수정합니다.

```text
Settings > Secrets and variables > Actions > Variables
```

### 3.3 서버 `.env`와 GitHub Secrets의 경계

GitHub Actions는 더 이상 `.env`를 생성하거나 덮어쓰지 않습니다. 아래 값들은 GitHub Secrets가 아니라 앱 GCE 인스턴스의 `~/app-dev/.env`에 둡니다.

```text
SPRING_PROFILES_ACTIVE
APP_PORT
MANAGEMENT_PORT
NODE_EXPORTER_PORT
LOG_LEVEL_ROOT
LOG_LEVEL_APP
APP_LOG_DIR
LOG_FILE_MAX_HISTORY
DB_PORT
POSTGRES_HOST
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
FLYWAY_ENABLED
FLYWAY_BASELINE_ON_MIGRATE
ADMIN_BOOTSTRAP_ENABLED
ADMIN_EMAIL
ADMIN_PASSWORD
JWT_SECRET
JWE_SECRET
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GCP_*
FIREBASE_*
```

GitHub Secrets에는 배포 접속/이미지 pull에 필요한 값만 둡니다.

```text
GCE_HOST
GCE_USER
GCE_SSH_KEY
GHCR_TOKEN
```

### 3.4 GCE 인스턴스 `.env` 준비

앱 GCE 인스턴스에서 아래 위치에 `.env` 파일을 만듭니다.

```bash
mkdir -p ~/app-dev
cd ~/app-dev
vi .env
```

PostgreSQL은 별도 DB GCE 인스턴스에서 실행하므로, `POSTGRES_HOST`는 DB 서버 private IP 또는 내부 DNS 이름으로 둡니다.

```env
SPRING_PROFILES_ACTIVE=prod
APP_PORT=8080
MANAGEMENT_PORT=9090
NODE_EXPORTER_PORT=9100
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=WARN
APP_LOG_DIR=./logs/app
LOG_FILE_MAX_HISTORY=30

POSTGRES_DB=geumjeongyahak
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
POSTGRES_HOST=DB_SERVER_PRIVATE_IP
POSTGRES_PORT=5432
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false

ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-this-strong-password-1A!
ADMIN_NAME=관리자

APP_IMAGE=ghcr.io/geumjeongyahak/backend:dev-latest
```

나머지 JWT, OAuth, GCP, Firebase 값은 `.env-example`을 기준으로 같은 파일에 추가합니다.
`ADMIN_PASSWORD`는 최초 관리자 계정 생성 후 `.env`에서 제거해도 됩니다. 이미 관리자 계정이 있으면 bootstrap은 비밀번호 없이도 재시작을 통과합니다.

앱 로그는 호스트의 `~/app-dev/logs/app`에 30일 rolling file로 저장됩니다. 앱 컨테이너는 UID/GID `1000`의 `spring` 유저로 실행되므로, 일반 GCE 배포 유저가 만든 디렉터리를 그대로 쓰는 구성이 기본입니다.

```bash
mkdir -p ~/app-dev/logs/app
```

DB GCE 인스턴스에는 별도 위치에 `.env` 파일을 만듭니다.

```bash
mkdir -p ~/db-dev
cd ~/db-dev
vi .env
```

```env
DB_PORT=5432
DB_BIND_ADDRESS=0.0.0.0
NODE_EXPORTER_PORT=9100

POSTGRES_DB=geumjeongyahak
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
POSTGRES_VERSION=16-alpine
```

PostgreSQL 데이터는 DB 서버 Docker volume인 `postgres-data`에 저장됩니다. `docker compose down`으로는 삭제되지 않지만, `docker compose down -v` 또는 volume 삭제를 실행하면 DB 데이터가 삭제됩니다.

### 3.5 홈서버 Prometheus/Alertmanager 운영

비용 최소화를 위해 Prometheus, Alertmanager, Grafana는 GCE가 아니라 홈서버에서 운영합니다. Cloud VPN, Cloud NAT, Load Balancer, Cloud Monitoring, Cloud Logging은 기본 구성에서 사용하지 않습니다.

앱 인스턴스는 애플리케이션 포트와 Actuator 포트를 분리합니다. 앱/DB 인스턴스의 시스템 메트릭은 Node Exporter로 수집합니다. 운영 단계에서 DB 내부 상태가 필요하면 DB 서버에 `postgres-exporter`를 추가할 수 있습니다.

```env
APP_PORT=8080
MANAGEMENT_PORT=9090
NODE_EXPORTER_PORT=9100
# optional, PostgreSQL 내부 메트릭이 필요해진 뒤 추가
POSTGRES_EXPORTER_PORT=9187
```

| 포트 | 대상 | 설명 |
| :--- | :--- | :--- |
| `9090` | 앱 GCE Spring Actuator | `/actuator/prometheus` |
| `9100` | 앱 GCE / DB GCE node-exporter | CPU, memory, disk, network metrics |
| `9187` | DB GCE postgres-exporter | PostgreSQL connection, lock, deadlock, DB size metrics |

기본 비용 최소 구성에서는 홈서버가 DB VM에 직접 접근하지 않습니다. App VM에 Caddy/Nginx monitoring gateway를 두고, 홈서버 Prometheus는 이 gateway만 scrape합니다.

```text
Home Prometheus
  -> App VM monitoring gateway :8443
      /app      -> 127.0.0.1:9090/actuator/prometheus
      /app-node -> 127.0.0.1:9100/metrics
      /db-node  -> DB_INTERNAL_IP:9100/metrics
      /postgres -> DB_INTERNAL_IP:9187/metrics, optional
```

GCE firewall은 `8443`만 홈서버 공인 IP에서 허용합니다. 홈서버에 DDNS가 있더라도 GCE firewall에는 hostname을 넣을 수 없으므로, DDNS를 resolve한 IP를 `/32`로 넣어야 합니다. 홈서버 cron에서 5~10분마다 firewall source range를 갱신합니다.

```bash
curl -4 ifconfig.me
dig +short A <HOME_DDNS_HOST>
```

두 값이 같으면 DDNS가 홈서버의 현재 outbound 공인 IP를 제대로 가리키는 것입니다. 라우터에서 홈서버에 고정 할당한 `192.168.x.x` 같은 내부 IP는 GCE firewall source range로 사용할 수 없습니다.

DDNS firewall 갱신 스크립트 예시:

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
  gcloud compute firewall-rules update "$RULE_NAME" \
    --project="$PROJECT_ID" \
    --source-ranges="$SOURCE_RANGE"
else
  gcloud compute firewall-rules create "$RULE_NAME" \
    --project="$PROJECT_ID" \
    --network="$NETWORK" \
    --allow="$PORTS" \
    --source-ranges="$SOURCE_RANGE" \
    --target-tags="$TARGET_TAG"
fi
```

cron 예시:

```cron
*/10 * * * * /opt/sonmoum/update-gce-monitoring-firewall.sh >> /var/log/sonmoum-gce-firewall-ddns.log 2>&1
```

Caddy gateway 예시:

```caddyfile
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

`9090`, `9100`, `9187`, `5432`는 public internet에 직접 열지 않습니다. DB VM의 `9100`/`9187`은 App VM network tag에서만 접근 가능하게 제한합니다.

홈서버에는 Prometheus, Alertmanager, Grafana를 둡니다. `prometheus.yml`의 target은 App VM gateway로 설정합니다.

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

postgres-exporter를 추가한 뒤에는 다음 job을 더합니다.

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

최소 알림 룰 예시:

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
```

Alertmanager는 Discord 또는 Telegram webhook으로 보냅니다. 알림 판단과 발송은 모두 홈서버에서 수행합니다.

GCE 방화벽 예시:

```bash
gcloud compute firewall-rules create allow-monitoring-gateway-from-home \
  --network=default \
  --allow=tcp:8443 \
  --source-ranges=HOME_SERVER_DDNS_RESOLVED_IP/32 \
  --target-tags=api-server

gcloud compute firewall-rules create allow-app-gateway-to-db-observability \
  --network=default \
  --allow=tcp:9100,tcp:9187 \
  --source-tags=api-server \
  --target-tags=db-server

gcloud compute firewall-rules create allow-app-to-postgres \
  --network=default \
  --allow=tcp:5432 \
  --source-ranges=APP_INSTANCE_PRIVATE_IP/32 \
  --target-tags=db-server
```

### 3.6 PR 머지 전 수동 배포 검증

머지 전에 현재 로컬 브랜치의 backend 이미지를 GHCR에 임시 태그로 push하고, GCE 인스턴스에서 pull/up 해서 직접 확인할 수 있습니다.

이 절차는 GitHub Actions를 우회해서 수동으로 검증하는 방법입니다. 서버의 `~/app-dev/.env`는 그대로 사용하고, 검증할 이미지 태그만 `APP_IMAGE`로 override합니다.

#### 3.6.1 로컬에서 이미지 build/push

현재 브랜치와 짧은 커밋 SHA로 검증용 태그를 만듭니다.

```bash
export IMAGE_TAG="$(git branch --show-current | tr '/' '-')-$(git rev-parse --short HEAD)"
export APP_IMAGE="ghcr.io/geumjeongyahak/backend:${IMAGE_TAG}"

docker login ghcr.io -u <github-username>
docker build -t "${APP_IMAGE}" -f infra/app/Dockerfile .
docker push "${APP_IMAGE}"
```

예시:

```text
ghcr.io/geumjeongyahak/backend:feature-channel-create-type-c9921cf
```

#### 3.6.2 gcloud로 compose/config 복사

로컬의 현재 파일을 앱 GCE 인스턴스의 `~/app-dev`로 복사합니다.

```bash
export GCE_INSTANCE=<app-instance-name>
export GCE_ZONE=<gce-zone>

gcloud compute ssh "${GCE_INSTANCE}" \
  --zone "${GCE_ZONE}" \
  --command "mkdir -p ~/app-dev"

gcloud compute scp \
  docker-compose.app.yml \
  docker-compose.dev.yml \
  Makefile \
  "${GCE_INSTANCE}:~/app-dev/" \
  --zone "${GCE_ZONE}"

```

#### 3.6.3 GCE에서 pull/up

GCE 인스턴스에서 GHCR 로그인 후, 방금 push한 이미지를 pull해서 올립니다.

```bash
gcloud compute ssh "${GCE_INSTANCE}" \
  --zone "${GCE_ZONE}" \
  --command "cd ~/app-dev && test -f .env && docker login ghcr.io -u <github-username> && APP_IMAGE=${APP_IMAGE} make deploy-app-dev"
```

`docker login`에서 토큰 입력이 번거롭다면 GCE에 접속해서 한 번만 로그인해도 됩니다.

```bash
gcloud compute ssh "${GCE_INSTANCE}" --zone "${GCE_ZONE}"
cd ~/app-dev
docker login ghcr.io -u <github-username>
APP_IMAGE=ghcr.io/geumjeongyahak/backend:<tag> make deploy-app-dev
```

#### 3.6.4 동작 확인

GCE 인스턴스에서 컨테이너 상태와 로그를 확인합니다.

```bash
gcloud compute ssh "${GCE_INSTANCE}" \
  --zone "${GCE_ZONE}" \
  --command "cd ~/app-dev && make ps-app-dev"

gcloud compute ssh "${GCE_INSTANCE}" \
  --zone "${GCE_ZONE}" \
  --command "cd ~/app-dev && docker compose -f docker-compose.app.yml -f docker-compose.dev.yml logs --tail=100 app"
```

로컬에서 API와 actuator health를 확인합니다.

```bash
export API_HOST=<app-external-ip-or-domain>

curl -f "http://${API_HOST}:8080/actuator/health"
curl -f "http://${API_HOST}:9090/actuator/health"
```

API 포트가 외부에 직접 열려 있지 않다면 SSH 터널로 확인합니다.

```bash
gcloud compute ssh "${GCE_INSTANCE}" \
  --zone "${GCE_ZONE}" \
  -- -L 18080:localhost:8080 -L 19090:localhost:9090

curl -f http://localhost:18080/actuator/health
curl -f http://localhost:19090/actuator/health
```

#### 3.6.5 기존 dev 이미지로 되돌리기

검증이 끝나면 현재 dev 배포 태그로 되돌립니다.

```bash
gcloud compute ssh "${GCE_INSTANCE}" \
  --zone "${GCE_ZONE}" \
  --command "cd ~/app-dev && APP_IMAGE=ghcr.io/geumjeongyahak/backend:dev-latest make deploy-app-dev"
```

#### 3.6.6 임시 이미지 정리

로컬과 GCE의 사용하지 않는 이미지는 정리할 수 있습니다.

```bash
docker image prune -f

gcloud compute ssh "${GCE_INSTANCE}" \
  --zone "${GCE_ZONE}" \
  --command "docker image prune -f"
```

---

## 4. 트러블슈팅

### 4.1 SSH 접속 권한 오류
GitHub Actions 실행 중 `Permission denied (publickey)` 오류가 발생한다면:
1.  `GCE_SSH_KEY` 시크릿에 Private Key가 정확히(줄바꿈 포함) 입력되었는지 확인합니다.
2.  GCE 인스턴스에 `GCE_USER`와 매칭되는 Public Key가 정상적으로 등록되었는지 확인합니다.

### 4.2 GHCR 이미지 Pull 오류
GCE 서버에서 `docker compose pull` 시 권한 오류가 발생한다면:
1.  `GHCR_TOKEN`의 권한에 `read:packages`가 포함되어 있는지 확인합니다.
2.  서버에서 직접 `docker login ghcr.io`를 수행하여 정상 로그인되는지 테스트합니다.
