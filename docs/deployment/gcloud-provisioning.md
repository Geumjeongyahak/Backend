# gcloud 기반 GCP 인프라 구성

Terraform 없이 `scripts/gcp/`의 idempotent shell script로 dev/prod App·DB GCE와 관측성을 구성한다. 전체 배포 순서는 루트 `DEPLOY.md`를 따른다.

## 구조

```text
App GCE e2-small
- Spring Boot jar + systemd
- Google Cloud Ops Agent
- optional internal Grafana

DB GCE e2-micro
- PostgreSQL + systemd
- postgres-exporter (localhost only)
- Google Cloud Ops Agent

Cloud Monitoring
- dev/prod dashboard
- API, VM, PostgreSQL metric alerts

Cloud Logging
- App WARN/ERROR logs
- 30-day _Default bucket retention
```

홈서버 Prometheus scrape와 OpenTelemetry 전송은 사용하지 않는다. Tailscale은 운영자 접근에만 사용한다.

## 준비

```bash
gcloud auth login
gcloud auth application-default login
cp scripts/gcp/00_env/dev.env.example scripts/gcp/00_env/dev.env
chmod 600 scripts/gcp/00_env/dev.env
```

환경 파일에서 프로젝트, 리전, 인스턴스, 스토리지, 도메인 값을 채운다. dev/prod는 가능하면 서로 다른 GCP 프로젝트를 사용한다.

## 인프라와 runtime env

```bash
scripts/gcp/01_infra/01_provision-gcp.sh scripts/gcp/00_env/dev.env

scripts/gcp/03_env_render/00_render-server-env.sh \
  scripts/gcp/00_env/dev.env app > scripts/gcp/00_env/dev.app.env
scripts/gcp/03_env_render/00_render-server-env.sh \
  scripts/gcp/00_env/dev.env db > scripts/gcp/00_env/dev.db.env
chmod 600 scripts/gcp/00_env/dev.app.env scripts/gcp/00_env/dev.db.env
```

생성된 runtime env의 `CHANGE_ME` 값을 실제 secret으로 교체한다. App에는 `ENVIRONMENT`, `MANAGEMENT_ADDRESS=127.0.0.1`, DB에는 `ENVIRONMENT`가 반드시 있어야 한다.

## 배포

```bash
RENDER_ENVS=false \
CONFIGURE_MONITORING=true \
scripts/gcp/07_deploy/00_deploy-env.sh scripts/gcp/00_env/dev.env
```

배포 스크립트는 App/DB Ops Agent를 설치하고 다음 localhost endpoint를 60초마다 수집한다.

- App: `127.0.0.1:9090/actuator/prometheus`
- DB: `127.0.0.1:9187/metrics`

node-exporter와 OpenTelemetry Java agent는 제거한다. Prometheus receiver의 allowlist 밖 지표와 불필요한 network/process/swap host 지표는 Cloud Monitoring으로 보내지 않는다.

## Cloud Monitoring

대시보드와 알림만 다시 적용하려면 다음을 실행한다.

```bash
scripts/gcp/06_observability/00_configure-cloud-monitoring.sh scripts/gcp/00_env/dev.env
```

알림 수신 채널은 환경 파일의 `ALERT_NOTIFICATION_CHANNELS`에 comma-separated resource name으로 지정한다.

## 선택형 내부 Grafana

기본값은 OFF다. App runtime env에서 활성화한다.

```env
INTERNAL_GRAFANA_ENABLED=true
INTERNAL_GRAFANA_ADMIN_PASSWORD=<16자 이상 비밀번호>
```

App 설치 스크립트를 다시 실행한 뒤 IAP SSH 터널로만 접속한다.

```bash
gcloud compute ssh "$APP_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap \
  -- -L 3000:127.0.0.1:3000
```

브라우저 주소는 `http://127.0.0.1:3000`이다. OFF로 되돌리면 Grafana 설정은 보존하고 systemd 서비스만 중지한다.

## 검증

```bash
sudo systemctl status google-cloud-ops-agent --no-pager
curl -fsS http://127.0.0.1:9090/actuator/prometheus >/dev/null
curl -fsS http://127.0.0.1:9187/metrics >/dev/null
```

Cloud Monitoring Metrics Management에서 allowlist 밖 Prometheus 지표가 새로 유입되지 않는지 확인한다. 전환 24시간 뒤 Billing의 Internet Data Transfer Out을 이전 24시간과 비교한다.
