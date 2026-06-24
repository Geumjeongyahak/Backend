# GCE 분리 배포와 PostgreSQL/Flyway 운영

## 목표 구성

```text
Client
  |
  v
App GCE e2-small
  - Spring Boot app
  - node-exporter
  |
  | private IP:5432
  v
DB GCE e2-micro
  - PostgreSQL
  - node-exporter
```

Spring Boot 애플리케이션과 PostgreSQL을 서로 다른 GCE 인스턴스에서 실행한다. 앱 서버는 `POSTGRES_HOST`에 DB 서버 private IP 또는 내부 DNS 이름을 넣어 접속한다.

## Compose 파일

- 앱 서버: `docker-compose.app.yml`, `docker-compose.dev.yml`
- DB 서버: `docker-compose.db.yml`
- 로컬/레거시 단일 구성: `docker-compose.yml`, `docker-compose.local.yml`

## 환경 변수

앱 서버 `~/app-dev/.env`의 필수 DB 접속 변수:

- `POSTGRES_HOST`: DB 서버 private IP 또는 내부 DNS
- `POSTGRES_PORT`: 기본 `5432`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_OPTIONS`
- `FLYWAY_ENABLED`
- `FLYWAY_BASELINE_ON_MIGRATE`: 신규 운영 DB는 `false`
- `ADMIN_BOOTSTRAP_ENABLED`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`: 최초 관리자 생성 후 제거 가능

DB 서버 `~/db-dev/.env`의 필수 DB 실행 변수:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_VERSION`
- `DB_PORT`
- `DB_BIND_ADDRESS`

`DB_BIND_ADDRESS=0.0.0.0`로 두더라도 GCE 방화벽에서 `5432` 접근은 앱 서버 private IP로만 제한한다.

## 배포 절차

1. GCE `e2-small` 앱 인스턴스와 `e2-micro` DB 인스턴스를 같은 VPC/리전에 생성한다.
2. 두 인스턴스에 Docker와 Docker Compose 플러그인을 설치한다.
3. DB 인스턴스의 private IP를 확인한다.
4. DB 인스턴스 `~/db-dev/.env`에 PostgreSQL 값을 채운다.
5. DB 인스턴스에 `docker-compose.db.yml`, `Makefile`을 복사하고 `make deploy-db-dev`를 실행한다.
6. 앱 인스턴스 `~/app-dev/.env`의 `POSTGRES_HOST`를 DB private IP로 설정한다.
7. 앱 인스턴스 `~/app-dev/.env`에 `ADMIN_EMAIL`, `ADMIN_PASSWORD`를 설정한다.
8. 앱 인스턴스에 `docker-compose.app.yml`, `docker-compose.dev.yml`, `Makefile`을 복사하고 `APP_IMAGE=... make deploy-app-dev`를 실행한다.
9. 앱 로그에서 DB 연결, Flyway 실행, 관리자 bootstrap 결과를 확인한다.
10. 최초 관리자 생성 후 `ADMIN_PASSWORD`를 `.env`에서 제거하고 앱을 재시작해도 된다.
11. `make ps-app-dev`, `make ps-db-dev`로 컨테이너 상태를 확인한다.

## 방화벽 기준

- 외부 공개: 앱 서버의 HTTP/HTTPS 진입 포트만 허용한다.
- DB 포트: `tcp:5432`는 앱 서버 private IP 또는 앱 서버 network tag에서만 허용한다.
- 모니터링 포트: `9090`, `9100`은 홈서버 VPN IP 또는 고정 IP에서만 허용한다.
- SSH: 운영자 또는 GitHub Actions 배포 IP 범위로 제한한다.

DB 방화벽 예시:

```bash
gcloud compute firewall-rules create allow-app-to-postgres \
  --network=default \
  --allow=tcp:5432 \
  --source-ranges=APP_SERVER_PRIVATE_IP/32 \
  --target-tags=db-server
```

## Flyway 운영 원칙

- 마이그레이션 파일은 `src/main/resources/db/migration` 아래에 둔다.
- 파일명은 `V{version}__{description}.sql` 형식을 사용한다.
- 한번 공유 브랜치에 반영된 마이그레이션 파일은 수정하지 않고 새 버전 파일을 추가한다.
- 운영 프로필은 JPA `ddl-auto=validate`를 사용하고, 스키마 생성/변경은 Flyway가 담당한다.
- 테스트 프로필은 기존 H2 초기화 흐름을 유지하기 위해 Flyway를 비활성화한다.
- `spring.sql.init`은 운영에서 비활성화한다.
- `flyway.clean`은 운영에서 비활성화한다.
- `FLYWAY_BASELINE_ON_MIGRATE=true`는 기존 스키마를 Flyway로 편입하는 일회성 작업에만 사용한다.

## 백업과 복구

DB 서버에서 수동 백업:

```bash
cd ~/db-dev
docker compose -f docker-compose.db.yml exec db pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > backup.sql
```

복구:

```bash
cd ~/db-dev
docker compose -f docker-compose.db.yml exec -T db psql -U "$POSTGRES_USER" "$POSTGRES_DB" < backup.sql
```

운영에서는 최소 1일 1회 백업을 VM 외부 저장소에 보관한다.

## 장애 대응

앱의 DB 연결 실패 시:

1. 앱 서버 `.env`의 `POSTGRES_HOST`, `POSTGRES_PORT`, 계정 값을 확인한다.
2. 앱 서버에서 `nc -vz DB_SERVER_PRIVATE_IP 5432`로 네트워크 접근을 확인한다.
3. GCE 방화벽이 앱 서버 private IP에서 DB 서버 `5432`로의 접근을 허용하는지 확인한다.
4. DB 서버에서 `make logs-db-dev`로 PostgreSQL 로그를 확인한다.

Flyway 실패 시:

1. 앱 서버에서 `make logs-app-dev`로 실패한 버전과 SQL 에러를 확인한다.
2. DB 백업이 있으면 복구 가능 여부를 먼저 판단한다.
3. 이미 적용된 마이그레이션 파일을 수정하지 않고, 실패 원인을 해결하는 새 마이그레이션을 작성한다.
4. 초기 배포처럼 데이터가 없는 환경이면 DB volume 제거 후 재생성이 가능하지만, 운영 데이터가 있으면 volume 삭제를 금지한다.
