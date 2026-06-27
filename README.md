# 금정야학 (GeumjeongYahak API)

금정열린배움터 교육 봉사 관리 시스템 백엔드 API

## 프로젝트 소개

금정야학은 교육 봉사자와 학생 간의 수업 매칭 및 관리를 위한 플랫폼입니다. 수기로 처리하던 출결, 일정, 결석, 재정 관리를 자동화하고, 수업 교환 절차를 간소화하며, 학습 진도를 체계적으로 관리할 수 있도록 지원합니다.

### 주요 기능

- **수업 관리** - 분반/과목/수업 CRUD, 과목 등록 시 수업 자동 생성
- **출결 관리** - 봉사자/학생 출석 체크 (출석, 결석, 지각, 조퇴, 공결)
- **요청 처리** - 결석/수업교환/구입 요청 및 승인
- **수업 일지** - 수업별 리뷰 작성 및 조회
- **인증/인가** - JWT Access/Refresh Token, 역할 기반 접근 제어 (RBAC)

## 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.x |
| Build | Gradle | 8.x |
| ORM | Spring Data JPA | - |
| Security | Spring Security + OAuth2 + JWT | - |
| DB (prod) | PostgreSQL | - |
| DB (test) | H2 | - |
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.8.x |
| Test | JUnit 5 + Rest Assured | - |

## 시작하기

### 사전 요구사항

- Java 21+
- PostgreSQL (개발/운영 환경) 또는 H2 (로컬 테스트)

### 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성합니다. `.env-example`을 참고하세요.

```env
# 필수 - JWT
JWT_SECRET=your-jwt-secret-key-min-256-bits
JWT_ACCESS_EXP_SECONDS=3600          # Access Token 만료 시간 (1시간)
JWT_REFRESH_EXP_SECONDS=1209600      # Refresh Token 만료 시간 (14일)

# 필수 - Spring Profile
SPRING_PROFILES_ACTIVE=dev           # local, dev, prod

# 필수 - 데이터베이스 (dev/prod)
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=gjlearn
POSTGRES_USER=your-db-user
POSTGRES_PASSWORD=your-db-password

# 선택 - CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# 선택 - OAuth2 (향후 구현 예정)
# GOOGLE_CLIENT_ID=your-google-client-id
# GOOGLE_CLIENT_SECRET=your-google-client-secret
```

### 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

실행 후 API 문서는 http://localhost:8080/swagger-ui.html 에서 확인할 수 있습니다.

### 테스트 컨벤션

PR 생성 및 머지 전에는 로컬 또는 별도 검증 환경에서 `./gradlew test`를 반드시 통과시킵니다.
GCE 배포는 Docker 이미지가 아니라 `bootJar` 산출물(`build/libs/*.jar`)을 앱 서버에 복사한 뒤 systemd 서비스로 실행합니다.

### Docker Compose

로컬에서는 앱과 PostgreSQL을 함께 빌드해서 실행합니다. GCE 운영 배포에는 Docker/Compose를 사용하지 않습니다.

```bash
cp .env-example .env
make up-local
make logs-local
make down-local
```

GCE dev/prod 배포는 앱 서버와 DB 서버를 분리합니다. 앱 서버는 Spring Boot jar + systemd, DB 서버는 apt PostgreSQL + systemd로 실행합니다.

```bash
./gradlew bootJar -x test
gcloud compute scp build/libs/*.jar scripts/gcp/05_app/01_install-app-service.sh \
  "$APP_INSTANCE_NAME:~/app-dev/" --project "$PROJECT_ID" --zone "$ZONE"
gcloud compute ssh "$APP_INSTANCE_NAME" --project "$PROJECT_ID" --zone "$ZONE" \
  --command "cd ~/app-dev && mv *.jar app.jar && chmod +x 01_install-app-service.sh && ./01_install-app-service.sh"
```

DB 서버는 최초 구성 또는 PostgreSQL/exporter 재설정 시 별도로 구성합니다.

```bash
gcloud compute scp scripts/gcp/04_db/01_install-db-service.sh scripts/gcp/00_env/dev.db.env \
  "$DB_INSTANCE_NAME:~/db-dev/" --project "$PROJECT_ID" --zone "$ZONE" --tunnel-through-iap
gcloud compute ssh "$DB_INSTANCE_NAME" --project "$PROJECT_ID" --zone "$ZONE" --tunnel-through-iap \
  --command "cd ~/db-dev && mv dev.db.env .env && chmod +x 01_install-db-service.sh && ./01_install-db-service.sh"
```

수동 빌드 전에 코드 검증이 필요하면 배포 명령과 분리해서 먼저 실행합니다.

```bash
./gradlew test
```

### 배포 구성

현재 dev/prod 배포는 두 개의 GCE 인스턴스와 홈서버 Tailscale 연동을 전제로 합니다. EC2라는 표현을 쓰더라도 여기서는 같은 역할의 App VM을 의미합니다.

```text
Home server (Prometheus/Grafana)
  ↕ Tailscale 100.64.0.0/10 또는 MagicDNS
App GCE e2-small
- Spring Boot jar systemd service
- node-exporter systemd service
- tailscaled

DB GCE e2-micro
- PostgreSQL systemd service
- node-exporter systemd service
- postgres-exporter systemd service
- tailscaled
```

각 GCE의 `.env`는 인스턴스 파일로 직접 관리합니다. 배포 자동화는 Tailscale SSH로 앱 서버에 접속해서 jar와 설치 스크립트만 갱신하고, `.env`는 덮어쓰지 않습니다.

앱 서버 필수 환경 변수 예시:

```env
SPRING_PROFILES_ACTIVE=prod
APP_PORT=8080
MANAGEMENT_PORT=9090
NODE_EXPORTER_PORT=9100
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=WARN
APP_LOG_DIR=./logs/app
LOG_FILE_PATTERN=./logs/app/application.%d{yyyy-MM-dd}.log
LOG_UPLOAD_PATH=./logs/app/application.*.log
LOG_FILE_MAX_HISTORY=30
LOG_FILE_TOTAL_SIZE_CAP=1GB
CLOUD_LOGGING_ENABLED=true
CLOUD_LOGGING_LOG_ID=gjlearn-prod-app

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
```

`ADMIN_PASSWORD`는 최초 관리자 계정 생성에만 필요합니다. 계정 생성 후에는 서버 `.env`에서 제거해도 재시작이 가능합니다. 기존 수동 생성 DB를 Flyway로 편입해야 하는 경우에만 `FLYWAY_BASELINE_ON_MIGRATE=true`를 일회성으로 사용하고, 신규 운영 DB는 `false`를 유지합니다.

DB 서버 필수 환경 변수 예시:

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

`POSTGRES_HOST`를 DB 서버 VPC private IP로 설정하면 PostgreSQL `pg_hba.conf`의 `APP_DB_CIDR`도 App VM의 VPC private IP `/32`여야 합니다. Tailscale IP/MagicDNS로 DB에 접속하는 구성에서만 App VM Tailscale IP `/32` 또는 Tailnet 대역을 사용합니다.

### 관측성

앱은 Spring Actuator 메트릭 endpoint를 노출하고, 앱/DB 서버는 Node Exporter로 시스템 메트릭을 노출합니다. DB 서버는 PostgreSQL Exporter도 함께 실행합니다. 홈서버 Prometheus는 public IP가 아니라 Tailscale IP 또는 MagicDNS hostname으로 scrape합니다. 앱 파일 로그는 `application.yyyy-MM-dd.log` 형식으로 일자별 저장하고 Logback이 30일 이후 파일을 정리합니다. 파일에는 `WARN`/`ERROR` 이상만 기록하므로 Cloud Ops Agent는 JSON 파싱 없이 해당 파일을 Cloud Logging으로 전달합니다.

| 포트 | 대상 | 설명 |
|------|------|------|
| `9090` | App GCE Spring Actuator | `/actuator/prometheus` |
| `9100` | App GCE / DB GCE node-exporter | CPU, memory, disk, network metrics |
| `9187` | DB GCE postgres-exporter | PostgreSQL metrics |

관측성 설정은 Backend repo의 `infra/monitoring`에서 수정하고, 홈서버 운영 경로인 `/home/min/Infra/monitoring`로 동기화합니다. 금정야학 dev target은 아래 파일에서 관리합니다.

```text
infra/monitoring/prometheus/targets/gjlearn/dev/app-actuator.yml
infra/monitoring/prometheus/targets/gjlearn/dev/node-exporter.yml
infra/monitoring/prometheus/targets/gjlearn/dev/postgres-exporter.yml
```

prod는 실제 prod App/DB tailnet 노드가 생긴 뒤 `infra/monitoring/prometheus/targets/gjlearn/prod/`에 같은 형식으로 추가합니다. `9090`, `9100`, `9187`, `5432`는 public internet에 직접 열지 않습니다. GCP firewall/security group에는 Tailscale용 `41641/udp`만 public 허용하면 됩니다.

repo 안에서 독립 실행:

```bash
make up-monitoring
make down-monitoring
```

홈서버 운영 경로에 반영:

```bash
make sync-monitoring-diff
make sync-monitoring-push
/home/min/Infra/monitoring/scripts/restart.sh
```

- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Alertmanager: `http://localhost:9093`
- Discord webhook은 `infra/monitoring/secrets/alertmanager/...` 또는 `/home/min/Infra/monitoring/secrets/alertmanager/...`에 둡니다. 개인용 실행 스크립트는 `scripts/local/`에 두면 git에 올라가지 않습니다.

## 아키텍처

모듈화된 도메인 기반 레이어드 아키텍처와 이벤트 기반 도메인 통신을 사용합니다.

```
┌──────────────────────────────────────────┐
│         Presentation Layer               │
│     (Controller, Request/Response DTO)   │
├──────────────────────────────────────────┤
│         Application Layer                │
│       (Service, Event Handler)           │
├──────────────────────────────────────────┤
│           Domain Layer                   │
│     (Entity, Repository Interface)       │
├──────────────────────────────────────────┤
│        Infrastructure Layer              │
│   (Repository Impl, Config, Security)    │
└──────────────────────────────────────────┘
```

### 프로젝트 구조

```
src/main/java/geumjeongyahak/
├── common/              # 공통 모듈
│   ├── advice/          #   전역 예외 처리
│   ├── config/          #   설정 (Swagger 등)
│   ├── event/           #   이벤트 정의
│   ├── exception/       #   공통 예외
│   ├── security/        #   보안 (JWT, Filter, Config)
│   └── validation/      #   커스텀 검증
│
└── domain/              # 도메인 레이어
    ├── base/            #   Base DTO, Entity
    │
    ├── auth/            #   인증/권한
    │   ├── entity/      #     Role, RefreshToken
    │   ├── enums/       #     RoleType
    │   ├── repository/
    │   ├── service/
    │   └── v1/          #     API v1
    │       ├── controller/
    │       └── dto/
    │
    ├── users/           #   사용자
    │   ├── entity/      #     User, UserRole
    │   ├── exception/
    │   ├── repository/
    │   ├── service/
    │   └── v1/          #     API v1
    │       ├── controller/
    │       └── dto/
    │
    ├── classroom/       #   분반
    │   ├── entity/
    │   ├── repository/
    │   └── v1/
    │
    ├── student/         #   학생
    │   ├── entity/
    │   ├── repository/
    │   └── v1/
    │
    ├── subject/         #   과목
    │   ├── entity/
    │   ├── event/       #     SubjectCreatedEvent
    │   ├── repository/
    │   └── v1/
    │
    ├── lesson/          #   수업
    │   ├── entity/
    │   ├── repository/
    │   └── v1/
    │
    └── request/         #   요청 (결석, 교환, 구입)
        ├── entity/
        ├── repository/
        └── v1/
```

### 도메인 이벤트

도메인 간 직접 호출 대신 Spring ApplicationEvent를 통해 느슨한 결합을 유지합니다.

| 이벤트 | 발행 | 수신 | 설명 |
|--------|------|------|------|
| SubjectCreatedEvent | Subject | Lesson | 과목 생성 시 수업 자동 생성 |
| LessonCreatedEvent | Lesson | Attendance | 수업 생성 시 출석 레코드 생성 |
| AbsenceApprovedEvent | Request | Lesson | 결석 승인 시 출석 상태 변경 |
| ExchangeApprovedEvent | Request | Lesson/Subject | 교환 승인 시 담당자 변경 |

**예시:**
```java
// 이벤트 발행
eventPublisher.publishEvent(new SubjectCreatedEvent(subjectId, ...));

// 이벤트 수신
@EventListener
@Transactional
public void handleSubjectCreated(SubjectCreatedEvent event) { ... }
```

## API 개요

기본 경로: `/api/v1`

| 도메인 | 엔드포인트 | 설명 |
|--------|-----------|------|
| Auth | `/auth/login`, `/auth/signup`, `/auth/refresh`, `/auth/logout` | 인증 (로그인, 회원가입, 토큰 재발급) |
| Auth | `/auth/me`, `/auth/me/logout-all` | 현재 사용자 정보, 전체 디바이스 로그아웃 |
| Auth | `/auth/password/change`, `/auth/password/forgot`, `/auth/password/reset` | 비밀번호 관리 |
| Auth | `/auth/check/username/{username}`, `/auth/check/email/{email}` | 중복 확인 |
| Users | `/users/**` | 사용자 CRUD (관리자 전용) |
| Users | `/users/{id}/roles` | 사용자 역할 추가/제거 (관리자 전용) |
| Classrooms | `/classrooms/**` | 분반 관리 |
| Students | `/students/**` | 학생 관리 |
| Subjects | `/subjects/**` | 과목 관리 |
| Lessons | `/lessons/**` | 수업/출결/일지 관리 |
| Requests | `/absence-requests/**`, `/purchase-requests/**` 등 | 요청 관리 |

### 응답 형식

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

## 권한 체계

### 기본 역할

| 역할 | Authority | 설명 |
|------|-----------|------|
| ADMIN | ROLE_ADMIN | 관리자 - 시스템 전체 관리, 요청 승인/거절 |
| MANAGER | ROLE_MANAGER | 매니저 - 중간 관리자 |
| VOLUNTEER | ROLE_VOLUNTEER | 봉사자 - 수업 진행, 출결 관리, 요청 생성 |
| GUEST | ROLE_GUEST | 게스트 - 제한적 접근 |

### 부서/교육 역할

| 역할 | Authority | 설명 |
|------|-----------|------|
| DEPT_FINANCE | DEPT_FINANCE | 재정 부서 |
| DEPT_ACADEMIC | DEPT_ACADEMIC | 학사 부서 |
| DEPT_IT | DEPT_IT | IT 부서 |
| TEACHER | TEACHER | 교사 |

**특징:**
- 한 사용자는 여러 역할을 동시에 가질 수 있음
- 기본 역할은 `ROLE_` prefix 사용 (`hasRole('ADMIN')`)
- 부서/교육 역할은 prefix 없이 사용 (`hasAuthority('TEACHER')`)

## 개발 컨벤션

### 커밋 메시지

```
<type>(<scope>): <subject>
```

- **Type**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- **Scope**: `user`, `lesson`, `subject`, `student`, `classroom`, `request`, `auth`, `global`

```
feat(lesson): 수업 캘린더 조회 API 추가
fix(auth): 로그인 시 세션 만료 오류 수정
```

### 브랜치 전략

```
main
 └── develop
      ├── feature/{issue-number}-{feature-name}
      ├── fix/{issue-number}-{bug-description}
      └── hotfix/{issue-number}-{description}
```

## 문서

| 문서 | 경로 | 설명 |
|------|------|------|
| PRD | [docs/prd.md](docs/prd.md) | 제품 요구사항 |
| 기술 명세 | [docs/tech_spec.md](docs/tech_spec.md) | 기술 설계 |
| API 명세 | [docs/api-spec/api_spec.md](docs/api-spec/api_spec.md) | API 흐름 (시퀀스 다이어그램) |
| 데이터 모델 | [docs/data_model.md](docs/data_model.md) | DB 스키마 |
| 개발 컨벤션 | [docs/convention/convention.md](docs/convention/convention.md) | 코드/커밋/브랜치 규칙 |
