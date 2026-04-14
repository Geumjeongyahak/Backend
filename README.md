# 금정야학 (GeumjeongYahak API)

금정열린배움터 교육 봉사 관리 시스템 백엔드 API

## 프로젝트 소개

금정야학은 교육 봉사자와 학생 간의 수업 매칭 및 관리를 위한 플랫폼입니다. 수기로 처리하던 출결, 일정, 결석, 재정 관리를 자동화하고, 수업 교환 절차를 간소화하며, 학습 진도를 체계적으로 관리할 수 있도록 지원합니다.

### 주요 기능

- **수업 관리** - 분반/과목/수업 CRUD, 과목 등록 시 수업 자동 생성
- **출결 관리** - 봉사자/학생 출석 체크 (출석, 결석, 지각, 조퇴, 공결)
- **요청 처리** - 결석/수업교환/과목교환/구입 요청 및 승인
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
POSTGRES_DB=sonmoum
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
