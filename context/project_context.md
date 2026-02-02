# 손모음 플랫폼 (Sonmoum API) Project Context

> This file is based on and referenced from `.claude/CLAUDE.md`.

## 프로젝트 개요

- **목적**: 교육 봉사자와 학생 간의 수업 매칭 및 관리
- **주요 기능**: 수업 일정 관리, 출석 관리, 요청 처리(결석/교환/구입)
- **문서 위치**: `docs/` 폴더 참조

## 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.x |
| Build | Gradle | 8.x |
| ORM | Spring Data JPA | - |
| Security | Spring Security + OAuth2 | - |
| DB (prod) | PostgreSQL | - |
| DB (dev) | H2 | - |
| API Docs | SpringDoc OpenAPI | 2.8.x |

## 아키텍처

**모듈화된 도메인 기반 레이어드 아키텍처** + **이벤트 기반 도메인 통신**

### 패키지 구조

```
src/main/java/org/geumjeong/learning/sonmoum_api/
├── common/                     # 공통 모듈 (구 global)
│   ├── config/                 # SwaggerConfig 등
│   ├── property/               # 설정 프로퍼티
│   ├── security/               # Security 설정
│   │   ├── config/             # WebSecurityConfig
│   │   ├── filter/             # Security Filter
│   │   ├── handler/            # Success/Failure Handler
│   │   └── property/           # Security Property
│   └── validation/             # 커스텀 Validation
│       ├── annotation/         # @ValidEmail 등
│       └── validator/          # Validator 구현체
│
├── api/                        # API 레이어 (Controller + DTO)
│   └── v1/                     # API v1
│       ├── auth/               # 인증 API
│       │   └── dto/            # Request/Response DTO
│       ├── common/             # 공통 API DTO
│       │   └── dto/            # BasePageRequest/Response
│       ├── departments/        # 부서 API
│       │   └── dto/
│       └── users/              # 사용자 API
│           └── dto/
│
├── domain/                     # 도메인 레이어
│   ├── base/                   # 공통 엔티티
│   │   └── entity/             # BaseEntity
│   ├── auth/                   # 인증/권한
│   │   ├── entity/             # Permission, UserPermission, DepartmentPermission
│   │   ├── enums/              # RoleType, PermissionType, ProviderType
│   │   ├── repository/
│   │   └── service/
│   ├── department/             # 부서
│   │   ├── entity/             # Department, UserDepartment
│   │   ├── repository/
│   │   └── service/
│   ├── users/                  # 사용자
│   │   ├── entity/             # User
│   │   ├── repository/
│   │   └── service/
│   ├── classroom/              # 분반 (예정)
│   ├── student/                # 학생 (예정)
│   ├── subject/                # 과목 (예정)
│   ├── lesson/                 # 수업 (예정)
│   └── request/                # 요청 (예정)
│
└── SonmoumApiApplication.java
```

### 도메인별 패키지 구조

```
api/v1/{도메인명}/
└── dto/           # Request/Response DTO
    ├── request/
    └── response/

domain/{도메인명}/
├── entity/        # @Entity
├── repository/    # @Repository
├── service/       # @Service
└── (enums/)       # Enum (필요시)
```

## 핵심 패턴

### 1. 이벤트 기반 도메인 통신

도메인 간 직접 호출 금지. Spring ApplicationEvent 사용.

```java
// 이벤트 정의 (record 사용)
public record SubjectCreatedEvent(Long subjectId, Long classroomId, ...) {}

// 이벤트 발행
eventPublisher.publishEvent(new SubjectCreatedEvent(...));

// 이벤트 수신
@EventListener
@Transactional
public void handleSubjectCreated(SubjectCreatedEvent event) { ... }
```

### 2. 통일된 응답 형식

```java
// 성공
ApiResponse.success(data)

// 실패
ApiResponse.error(ErrorCode.USER_NOT_FOUND)
```

### 3. BaseEntity 상속

모든 엔티티는 `BaseEntity` 상속 (createdAt, updatedAt 자동 관리)

```java
@Entity
public class User extends BaseEntity { ... }
```

## 코드 컨벤션

### 커밋 메시지 (Conventional Commits)

```
<type>(<scope>): <subject>
```

**Type**: feat, fix, docs, style, refactor, test, chore

**Scope**: user, lesson, subject, student, classroom, request, auth, global

**예시**:
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

### 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `UserService` |
| 메서드/변수 | camelCase | `getUserById` |
| 상수 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| DB 테이블 | snake_case, 복수형 | `users`, `lessons` |
| DB 컬럼 | snake_case | `created_at` |

## 주요 도메인 관계

| 이벤트 | 발행 | 수신 | 설명 |
|--------|------|------|------|
| SubjectCreatedEvent | Subject | Lesson | 과목 생성 → 수업 자동 생성 |
| LessonCreatedEvent | Lesson | Attendance | 수업 생성 → 출석 레코드 생성 |
| AbsenceApprovedEvent | Request | Lesson | 결석 승인 → 출석 상태 변경 |

## 권한 체계

```java
public enum Role {
    VOLUNTEER,  // 봉사자
    ADMIN       // 관리자
}
```

## 주요 명령어

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 실행 (local 프로필)
./gradlew bootRun

# API 문서 확인
# http://localhost:8080/swagger-ui.html
```

## 참조 문서

- [PRD](docs/prd.md) - 제품 요구사항
- [Tech Spec](docs/tech_spec.md) - 기술 명세
- [API Spec](docs/api_spec.md) - API 흐름 (시퀀스 다이어그램)
- [Data Model](docs/data_model.md) - 데이터 모델
- [Convention](docs/convention/) - 개발 컨벤션
