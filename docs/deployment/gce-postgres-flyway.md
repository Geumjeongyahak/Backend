# GCE PostgreSQL 배포와 Flyway 운영

## 목표 구성

```text
Spring Boot App
        |
        v
    Flyway
        |
        v
   PostgreSQL
```

GCE `e2-small` 단일 VM에서 `docker-compose.yml`로 Spring Boot 애플리케이션과 PostgreSQL을 함께 실행한다.

## 환경 변수

`.env-example`을 기준으로 `.env`를 만들고 운영 값으로 교체한다.

필수 DB 변수:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_HOST`
- `POSTGRES_PORT`
- `POSTGRES_OPTIONS`
- `FLYWAY_ENABLED`

Compose 배포에서는 app 서비스가 `POSTGRES_HOST=db`, `POSTGRES_PORT=5432`를 주입한다. VM 외부에서 DB에 직접 접속해야 하는 경우에만 SSH 터널 또는 별도 방화벽 정책을 사용한다.

## 배포 절차

1. GCE `e2-small` 인스턴스를 생성한다.
2. Docker와 Docker Compose 플러그인을 설치한다.
3. 저장소를 배포 경로에 clone하거나 배포 산출물을 업로드한다.
4. `.env-example`을 `.env`로 복사한 뒤 운영 값을 채운다.
5. `docker compose up -d --build`를 실행한다.
6. `docker compose logs -f app`에서 Flyway 마이그레이션 성공 여부를 확인한다.
7. `docker compose ps`로 app과 db 상태가 healthy/up인지 확인한다.

## Flyway 운영 원칙

- 마이그레이션 파일은 `src/main/resources/db/migration` 아래에 둔다.
- 파일명은 `V{version}__{description}.sql` 형식을 사용한다.
- 한번 공유 브랜치에 반영된 마이그레이션 파일은 수정하지 않고 새 버전 파일을 추가한다.
- 운영 프로필은 JPA `ddl-auto=validate`를 사용하고, 스키마 생성/변경은 Flyway가 담당한다.
- 테스트 프로필은 기존 H2 초기화 흐름을 유지하기 위해 Flyway를 비활성화한다.

## 백업과 복구

수동 백업 예시:

```bash
docker compose exec db pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > backup.sql
```

복구 예시:

```bash
docker compose exec -T db psql -U "$POSTGRES_USER" "$POSTGRES_DB" < backup.sql
```

운영에서는 최소 1일 1회 백업을 VM 외부 저장소에 보관한다.

## 보안 기준

- PostgreSQL 포트는 기본적으로 외부에 노출하지 않는다.
- `docker-compose.yml`의 db 서비스는 app과 같은 Docker 네트워크 안에서만 접근한다.
- GCE 방화벽은 애플리케이션 HTTP/HTTPS 포트만 열고 DB 포트는 닫는다.
- `.env`는 저장소에 커밋하지 않는다.
- GCS, Firebase, OAuth, JWT 비밀값은 VM 환경 변수 또는 Secret Manager 주입을 우선한다.

## 장애 대응

Flyway 실패 시:

1. `docker compose logs app`에서 실패한 버전과 SQL 에러를 확인한다.
2. DB 백업이 있으면 복구 가능 여부를 먼저 판단한다.
3. 이미 적용된 마이그레이션 파일을 수정하지 않고, 실패 원인을 해결하는 새 마이그레이션을 작성한다.
4. 초기 배포처럼 데이터가 없는 환경이면 volume 제거 후 재생성이 가능하지만, 운영 데이터가 있으면 volume 삭제를 금지한다.
