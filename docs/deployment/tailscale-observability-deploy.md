# Tailscale 운영 접근과 Cloud 관측성

GJLearn 관측성 데이터는 홈서버로 전송하지 않는다. Tailscale은 운영자 SSH 접근에만 사용하며 App/DB Ops Agent가 localhost 지표를 Cloud Monitoring으로 전송한다.

## 수집 경로

```text
App VM
- Ops Agent CPU/memory/disk metrics
- 127.0.0.1:9090/actuator/prometheus 핵심 지표
- WARN/ERROR 파일 로그 -> Cloud Logging

DB VM
- Ops Agent CPU/memory/disk metrics
- 127.0.0.1:9187 PostgreSQL 핵심 지표
```

수집 주기는 60초이며 application/exporter allowlist 밖 지표와 불필요한 network/process/swap 지표는 전송하지 않는다. 기본 syslog와 Ops Agent self-log 수집도 비활성화해 App의 WARN/ERROR 파일 로그만 Cloud Logging으로 보낸다. `9090`, `9187`, `3000`은 public 또는 Tailscale interface에 bind하지 않는다.

## Cloud Monitoring 적용

```bash
scripts/gcp/06_observability/00_configure-cloud-monitoring.sh scripts/gcp/00_env/dev.env
scripts/gcp/06_observability/00_configure-cloud-monitoring.sh scripts/gcp/00_env/prod.env
```

스크립트는 Cloud Logging 30일 보관, dev/prod 대시보드, 메트릭 알림과 WARN/ERROR 로그 알림을 생성하거나 갱신한다.

## 내부 Grafana

App VM `.env`에서만 선택적으로 활성화한다.

```env
INTERNAL_GRAFANA_ENABLED=true
INTERNAL_GRAFANA_ADMIN_PASSWORD=<16자 이상 비밀번호>
```

설치 스크립트를 재실행한 뒤 IAP SSH 터널로 접속한다.

```bash
gcloud compute ssh "$APP_INSTANCE_NAME" \
  --project "$PROJECT_ID" \
  --zone "$ZONE" \
  --tunnel-through-iap \
  -- -L 3000:127.0.0.1:3000
```

브라우저 주소는 `http://127.0.0.1:3000`이다. `INTERNAL_GRAFANA_ENABLED=false`로 되돌리고 설치 스크립트를 재실행하면 데이터와 설정은 남기고 서비스만 중지한다.

## 확인

```bash
sudo systemctl status google-cloud-ops-agent --no-pager
curl -fsS http://127.0.0.1:9090/actuator/prometheus >/dev/null
curl -fsS http://127.0.0.1:9187/metrics >/dev/null
```

Cloud Monitoring Metrics Management에서 비-allowlist 지표가 유입되지 않는지 확인하고, 전환 24시간 뒤 Billing의 Internet Data Transfer Out을 전환 전과 비교한다.
