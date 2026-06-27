# Tailscale 기반 배포/모니터링 연동

이 문서는 홈서버 Prometheus/Grafana와 GCE App/DB 서버를 Tailscale로 연결하는 기준 절차다. EC2라는 표현을 쓰더라도 여기서는 동일 역할의 GCE App VM을 의미한다.

## 목표 구조

```text
Home server
- Prometheus
- Grafana

  ↕ Tailscale VPN (100.64.0.0/10 또는 MagicDNS)

App GCE e2-small
- Spring Boot jar :8080
- node-exporter :9100
- tailscaled

DB GCE e2-micro
- PostgreSQL :5432
- node-exporter :9100
- postgres-exporter :9187
- tailscaled
```

public internet에 직접 열어야 하는 것은 Tailscale 직접 연결용 `41641/udp`뿐이다. `8080`, `9100`, `9187`, `5432`는 Tailscale IP/hostname으로 접근한다.

금정야학 기본 운영 방식은 단순 Tailscale 노드 모드다. 각 GCE VM을 tailnet에 직접 붙이며, GCP 내부 서브넷 라우터로 쓰지 않는다. 따라서 VM 생성 시 `--can-ip-forward`를 켜지 않고, OS의 `net.ipv4.ip_forward`/IPv6 forwarding도 설정하지 않으며, `tailscale up`에 `--advertise-routes`를 붙이지 않는다.

## 1. 서버에 Tailscale 설치/인증

startup script와 설치 스크립트는 `tailscale` 패키지를 설치한다. GCE metadata startup script는 root로 실행된다. 이미 생성된 VM에서 `startup-app-manual.sh` 또는 `startup-db-manual.sh`를 직접 재실행할 때도 root 권한이 필요하므로 스크립트가 `sudo -E`로 자동 승격한다. 최초 인증은 각 서버에 SSH/IAP로 접속해 한 번 수행한다.

```bash
# App VM 예시
sudo tailscale up \
  --advertise-tags=tag:gjlearn-prod-app,tag:gjlearn-app,tag:prod \
  --accept-dns=false

# DB VM 예시
sudo tailscale up \
  --advertise-tags=tag:gjlearn-prod-db,tag:gjlearn-db,tag:prod \
  --accept-dns=false
```

`--accept-dns=false`는 GCE DNS를 유지하기 위한 기본값이다. install script를 사용할 경우 서버 `.env`에 `TAILSCALE_TAGS`와 `TAILSCALE_ACCEPT_DNS=false`를 넣으면 같은 설정으로 실행된다.

출력된 URL을 브라우저에서 열어 홈서버와 같은 Tailscale 계정으로 인증한다.

확인:

```bash
tailscale ip -4
tailscale status
```

MagicDNS를 사용할 경우 Tailscale Admin Console에서 MagicDNS를 켠 뒤 서버 이름을 확인한다.

```bash
tailscale status --json | jq -r '.Self.DNSName'
```

## 2. 홈서버 Prometheus 설정

Backend repo의 `infra/monitoring`은 편집/독립 실행용 mirror이고, 홈서버 운영 경로는 `/home/min/Infra/monitoring`이다.

```text
infra/monitoring/prometheus/targets/gjlearn/dev/app-actuator.yml
infra/monitoring/prometheus/targets/gjlearn/dev/node-exporter.yml
infra/monitoring/prometheus/targets/gjlearn/dev/postgres-exporter.yml
```

IP로 쓸 경우 각 target에 `tailscale ip -4` 결과인 `100.x.x.x:port`를 넣는다. IP 고정 관리가 싫으면 MagicDNS hostname을 우선 사용한다.

홈서버 Prometheus reload:

```bash
/home/min/Infra/monitoring/scripts/restart.sh
```

## 3. GCP firewall 기준

Tailscale 트래픽만 public ingress로 허용한다.

| 포트 | 소스 | 대상 | 용도 |
|------|------|------|------|
| `41641/udp` | `0.0.0.0/0` | App/DB VM | Tailscale WireGuard direct path |
| `22/tcp` | 관리자 IP 또는 IAP | App/DB VM | 최초 설정/비상 접근 |
| `8080/tcp` | 필요 시 public | App VM | API를 public으로 노출할 때만 |

열지 않는 포트:

| 포트 | 이유 |
|------|------|
| `9100` | Tailscale로만 node-exporter scrape |
| `9187` | Tailscale로만 postgres-exporter scrape |
| `5432` | App VM 또는 Tailscale 내부에서만 DB 접근 |

`scripts/gcp/01_infra/01_provision-gcp.sh`는 App/DB network tag에 대해 `41641/udp` firewall rule을 생성한다.

## 4. DB 접속 범위

DB 서버의 `~/db-dev/.env`는 앱이 DB에 접속할 때 DB가 보게 되는 source IP를 허용해야 한다. 렌더 스크립트가 만드는 기본 app env는 DB VPC private IP를 `POSTGRES_HOST`로 쓰므로, 기본 `APP_DB_CIDR`도 App VM VPC private IP `/32`다.

```env
APP_DB_CIDR=10.x.x.x/32
```

앱이 DB Tailscale IP/MagicDNS로 접속하도록 바꾼 경우에는 App VM의 Tailscale IP만 허용한다.

```env
APP_DB_CIDR=100.x.x.x/32
```

변경 후 DB 서버에서 재실행:

```bash
cd ~/db-dev
./01_install-db-service.sh
```

## 5. GitHub Actions 배포

CI/CD 배포는 Tailscale runner 연결 없이 SSH key로 App VM에 직접 접속한다. Tailscale은 홈서버 Prometheus scrape와 운영자 접근에만 사용한다.

GitHub Actions 배포는 Tailscale runner 연결 없이 SSH key로 App VM에 직접 접속한다. App VM의 SSH 포트는 public 전체가 아니라 운영자/CI 접근 범위로 제한한다.

GitHub Secrets:

| 이름 | 설명 |
|------|------|
| `DEV_GCE_HOST` | dev App VM SSH host. 없으면 기존 `GCE_HOST` fallback |
| `DEV_GCE_USER` | dev App VM SSH 사용자. 없으면 기존 `GCE_USER` fallback |
| `DEV_GCE_SSH_KEY` | dev App VM SSH private key. 없으면 기존 `GCE_SSH_KEY` fallback |
| `PROD_GCE_HOST` | prod App VM SSH host |
| `PROD_GCE_USER` | prod App VM SSH 사용자 |
| `PROD_GCE_SSH_KEY` | prod App VM SSH private key |

배포 trigger는 branch별로 분리한다.

| workflow | trigger | 대상 |
|----------|---------|------|
| `.github/workflows/deploy-dev.yml` | `dev` branch push/merge, manual dispatch | dev App VM |
| `.github/workflows/deploy-prod.yml` | `main` branch push/merge, manual dispatch | prod App VM |

각 workflow는 다음 순서로 실행된다.

1. `./gradlew bootJar -x test`
2. `scp`로 jar와 `scripts/gcp/05_app/01_install-app-service.sh`를 App VM에 복사
3. SSH로 `gjlearn-app.service`를 재시작

수동 배포도 동일한 SSH host/key 경로를 사용한다.

```bash
./gradlew bootJar -x test
scp build/libs/*.jar scripts/gcp/05_app/01_install-app-service.sh \
  min@gjlearn-app.<tailnet>.ts.net:~/app-dev/
ssh min@gjlearn-app.<tailnet>.ts.net \
  'cd ~/app-dev && mv *.jar app.jar && chmod +x 01_install-app-service.sh && ./01_install-app-service.sh'
```

서버 runtime env는 `/tmp`가 아니라 gitignore된 `scripts/gcp/00_env/{dev,prod}.app.env`, `scripts/gcp/00_env/{dev,prod}.db.env`에 저장한 뒤 각 VM의 `~/app-dev/.env`, `~/db-dev/.env`로 복사한다. JWT/JWE, Google OAuth, Firebase, DB password 값은 dev/prod를 분리한다.

## 6. 연결 확인

홈서버에서:

```bash
curl -fsS http://gjlearn-app.<tailnet>.ts.net:9090/actuator/health
curl -fsS http://gjlearn-app.<tailnet>.ts.net:9100/metrics >/dev/null
curl -fsS http://gjlearn-db.<tailnet>.ts.net:9100/metrics >/dev/null
curl -fsS http://gjlearn-db.<tailnet>.ts.net:9187/metrics >/dev/null
```

App VM에서 DB 확인:

```bash
nc -vz gjlearn-db.<tailnet>.ts.net 5432
```

문제가 생기면 먼저 각 서버에서 `tailscale status`와 `tailscale ip -4`를 확인한다.
