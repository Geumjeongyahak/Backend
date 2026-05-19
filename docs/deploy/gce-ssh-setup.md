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
| **`GCE_HOST`** | GCE 인스턴스의 외부 IP | `gcloud compute instances list` |
| **`GCE_USER`** | SSH 접속 계정명 | `gcloud compute ssh ... --command="whoami"` |
| **`GCE_SSH_KEY`** | 생성한 Private Key 전문 | `cat gce-deploy-key` (내용 전체) |
| **`GHCR_TOKEN`** | GitHub Personal Access Token | [GitHub PAT 설정](https://github.com/settings/tokens) (Classic 권한: `read:packages`, `write:packages`) |

`GCE_ENV_DEV`는 사용하지 않습니다. 애플리케이션 환경 변수는 GCE 인스턴스의 `~/app-dev/.env` 파일로 직접 관리합니다.

### 3.1 GCE 인스턴스 `.env` 준비

GCE 인스턴스에서 아래 위치에 `.env` 파일을 만듭니다.

```bash
mkdir -p ~/app-dev
cd ~/app-dev
vi .env
```

DB도 같은 인스턴스의 Docker Compose에서 함께 실행하므로, PostgreSQL host는 `db`로 둡니다.

```env
SPRING_PROFILES_ACTIVE=dev
APP_PORT=8080
DB_PORT=5432

POSTGRES_DB=geumjeongyahak
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
POSTGRES_HOST=db
POSTGRES_PORT=5432

APP_IMAGE=ghcr.io/geumjeongyahak/backend:dev-latest
```

나머지 JWT, OAuth, GCP, Firebase 값은 `.env-example`을 기준으로 같은 파일에 추가합니다.

PostgreSQL 데이터는 Docker volume인 `postgres-data`에 저장됩니다. `docker compose down`으로는 삭제되지 않지만, `docker compose down -v` 또는 volume 삭제를 실행하면 DB 데이터가 삭제됩니다.

### 3.2 Prometheus 분리 운영

API 인스턴스는 애플리케이션 포트와 관측 포트를 분리합니다.

```env
APP_PORT=8080
MANAGEMENT_PORT=9090
NODE_EXPORTER_PORT=9100
CADVISOR_PORT=8081
POSTGRES_EXPORTER_PORT=9187
ALLOY_PORT=12345
LOKI_PUSH_URL=http://MONITORING_PRIVATE_IP:3100/loki/api/v1/push
```

App/DB GCE에는 `infra/app-server/docker-compose.observability.yml`가 함께 올라갑니다.

| 포트 | 대상 | 설명 |
| :--- | :--- | :--- |
| `9090` | Spring Actuator | `/actuator/prometheus` |
| `9100` | node-exporter | GCE VM CPU, memory, disk, network |
| `8081` | cAdvisor | Docker container metrics |
| `9187` | postgres-exporter | PostgreSQL metrics |
| `12345` | Grafana Alloy | Alloy self metrics/status |

Prometheus는 별도 GCE 인스턴스에 두고, API 인스턴스의 관측성 포트들은 Prometheus 인스턴스의 사설 IP에서만 접근 가능하게 방화벽을 제한합니다. Alloy는 App/DB GCE의 Docker 로그를 읽어서 Monitoring GCE의 Loki로 push합니다.

Prometheus 인스턴스에는 `infra/monitoring/docker-compose.prometheus.yml`와 `infra/monitoring/prometheus.yml`를 복사한 뒤, `prometheus.yml`의 target을 API 인스턴스의 사설 IP로 바꿉니다.

```yaml
scrape_configs:
  - job_name: sonmoum-api
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - API_INSTANCE_PRIVATE_IP:9090
```

실행:

```bash
docker compose -f docker-compose.prometheus.yml up -d
```

GCE 방화벽 예시:

```bash
gcloud compute firewall-rules create allow-monitoring-to-api-observability \
  --network=default \
  --allow=tcp:9090,tcp:9100,tcp:8081,tcp:9187,tcp:12345 \
  --source-ranges=PROMETHEUS_INSTANCE_PRIVATE_IP/32 \
  --target-tags=api-server
```

### 3.3 PR 머지 전 수동 배포 검증

머지 전에 현재 로컬 브랜치의 backend 이미지를 GHCR에 임시 태그로 push하고, GCE 인스턴스에서 pull/up 해서 직접 확인할 수 있습니다.

이 절차는 GitHub Actions를 우회해서 수동으로 검증하는 방법입니다. 서버의 `~/app-dev/.env`는 그대로 사용하고, 검증할 이미지 태그만 `APP_IMAGE`로 override합니다.

#### 3.3.1 로컬에서 이미지 build/push

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

#### 3.3.2 gcloud로 compose/config 복사

로컬의 현재 파일을 App/DB GCE 인스턴스의 `~/app-dev`로 복사합니다.

```bash
export GCE_INSTANCE=<app-db-instance-name>
export GCE_ZONE=<gce-zone>

gcloud compute ssh "${GCE_INSTANCE}" \
  --zone "${GCE_ZONE}" \
  --command "mkdir -p ~/app-dev/src/main/resources/sql ~/app-dev/infra/app-server"

gcloud compute scp \
  docker-compose.yml \
  docker-compose.dev.yml \
  Makefile \
  "${GCE_INSTANCE}:~/app-dev/" \
  --zone "${GCE_ZONE}"

gcloud compute scp \
  src/main/resources/sql/init_scheme.sql \
  src/main/resources/sql/init_data.sql \
  "${GCE_INSTANCE}:~/app-dev/src/main/resources/sql/" \
  --zone "${GCE_ZONE}"

gcloud compute scp \
  --recurse infra/app-server \
  "${GCE_INSTANCE}:~/app-dev/infra/" \
  --zone "${GCE_ZONE}"
```

#### 3.3.3 GCE에서 pull/up

GCE 인스턴스에서 GHCR 로그인 후, 방금 push한 이미지를 pull해서 올립니다.

```bash
gcloud compute ssh "${GCE_INSTANCE}" \
  --zone "${GCE_ZONE}" \
  --command "cd ~/app-dev && test -f .env && docker login ghcr.io -u <github-username> && APP_IMAGE=${APP_IMAGE} make deploy-dev"
```

`docker login`에서 토큰 입력이 번거롭다면 GCE에 접속해서 한 번만 로그인해도 됩니다.

```bash
gcloud compute ssh "${GCE_INSTANCE}" --zone "${GCE_ZONE}"
cd ~/app-dev
docker login ghcr.io -u <github-username>
APP_IMAGE=ghcr.io/geumjeongyahak/backend:<tag> make deploy-dev
```

#### 3.3.4 동작 확인

GCE 인스턴스에서 컨테이너 상태와 로그를 확인합니다.

```bash
gcloud compute ssh "${GCE_INSTANCE}" \
  --zone "${GCE_ZONE}" \
  --command "cd ~/app-dev && make ps-dev"

gcloud compute ssh "${GCE_INSTANCE}" \
  --zone "${GCE_ZONE}" \
  --command "cd ~/app-dev && docker compose -f docker-compose.yml -f docker-compose.dev.yml -f infra/app-server/docker-compose.observability.yml logs --tail=100 app"
```

로컬에서 API와 actuator health를 확인합니다.

```bash
export API_HOST=<app-db-external-ip-or-domain>

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

#### 3.3.5 기존 dev 이미지로 되돌리기

검증이 끝나면 현재 dev 배포 태그로 되돌립니다.

```bash
gcloud compute ssh "${GCE_INSTANCE}" \
  --zone "${GCE_ZONE}" \
  --command "cd ~/app-dev && APP_IMAGE=ghcr.io/geumjeongyahak/backend:dev-latest make deploy-dev"
```

#### 3.3.6 임시 이미지 정리

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
