# 손모음 플랫폼 기술 명세서

## 1. 개요

### 1.1 목적

손모음 플랫폼 백엔드 시스템의 기술적 설계와 구현 방향을 정의합니다.

### 1.2 참조 문서

- [PRD (Product Requirements Document)](./prd.md)
- [API 명세서](./api_spec.md)
- [데이터 모델](./data_model.md)

---

## 2. 기술 스택

### 2.1 Core

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.x |
| Build Tool | Gradle | 8.x |
| ORM | Spring Data JPA | - |

### 2.2 Security

| 구분 | 기술 | 용도 |
|------|------|------|
| Authentication | Spring Security | 인증/인가 |
| OAuth2 | Spring OAuth2 Client | 소셜 로그인 (Google) |

### 2.3 Database

| 환경 | 기술 | 용도 |
|------|------|------|
| Production | PostgreSQL | 운영 DB |
| Development | H2 | 개발/테스트 DB |

### 2.4 Documentation & Monitoring

| 구분 | 기술 | 버전 |
|------|------|------|
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.8.x |
| Monitoring | Spring Actuator | - |

### 2.5 Utilities

| 구분 | 기술 | 용도 |
|------|------|------|
| Lombok | Lombok | 보일러플레이트 코드 제거 |
| Env | dotenv-java | 환경 변수 관리 |

---

## 3. 아키텍처

### 3.1 개요

**모듈화된 도메인 기반 레이어드 아키텍처**를 채택합니다.

- 도메인별 패키지 분리로 응집도 향상
- 도메인 간 통신은 **이벤트 기반**으로 처리하여 결합도 최소화
- 각 도메인은 독립적인 레이어 구조 유지

### 3.2 레이어 구조

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│              (Controller, DTO, Request/Response)         │
├─────────────────────────────────────────────────────────┤
│                    Application Layer                     │
│                (Service, Event Handler)                  │
├─────────────────────────────────────────────────────────┤
│                      Domain Layer                        │
│              (Entity, Repository Interface)              │
├─────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                   │
│        (Repository Impl, External API, Config)           │
└─────────────────────────────────────────────────────────┘
```

### 3.3 패키지 구조

```
src/main/java/org/geumjeong/learning/sonmoum_api/
├── common/                          # 공통 모듈 (구 global)
│   ├── config/                      # 설정 클래스
│   │   └── SwaggerConfig.java
│   ├── property/                    # 설정 프로퍼티
│   ├── security/                    # Security 관련
│   │   ├── config/
│   │   │   └── WebSecurityConfig.java
│   │   ├── filter/                  # Security Filter
│   │   ├── handler/                 # Success/Failure Handler
│   │   └── property/                # Security Property
│   └── validation/                  # 커스텀 Validation
│       ├── annotation/              # @ValidEmail 등
│       └── validator/               # Validator 구현체
│
├── api/                             # API 레이어 (Controller + DTO)
│   └── v1/                          # API v1
│       ├── auth/                    # 인증 API
│       │   └── dto/
│       │       ├── request/
│       │       └── response/
│       ├── common/                  # 공통 API DTO
│       │   └── dto/
│       │       ├── request/         # BasePageRequest
│       │       └── response/        # BasePageResponse
│       ├── departments/             # 부서 API
│       │   └── dto/
│       │       ├── request/
│       │       └── response/
│       └── users/                   # 사용자 API
│           └── dto/
│               ├── request/
│               └── response/
│
├── domain/                          # 도메인 레이어
│   ├── base/                        # 공통 엔티티
│   │   └── entity/
│   │       └── BaseEntity.java      # created_at, updated_at
│   │
│   ├── auth/                        # 인증/권한 도메인
│   │   ├── entity/
│   │   │   ├── Permission.java
│   │   │   ├── UserPermission.java
│   │   │   └── DepartmentPermission.java
│   │   ├── enums/
│   │   │   ├── RoleType.java        # VOLUNTEER, ADMIN
│   │   │   ├── PermissionType.java  # SUPER_ADMIN, MANAGE_USERS, ...
│   │   │   └── ProviderType.java    # EMAIL, GOOGLE
│   │   ├── repository/
│   │   └── service/
│   │       └── EmailAuthService.java
│   │
│   ├── department/                  # 부서 도메인
│   │   ├── entity/
│   │   │   ├── Department.java
│   │   │   └── UserDepartment.java
│   │   ├── repository/
│   │   │   └── DepartmentRepository.java
│   │   └── service/
│   │       └── DepartmentCrudService.java
│   │
│   ├── users/                       # 사용자 도메인
│   │   ├── entity/
│   │   │   └── User.java
│   │   ├── repository/
│   │   │   └── UserRepository.java
│   │   └── service/
│   │       ├── UserCrudService.java
│   │       └── UserPermissionService.java
│   │
│   ├── classroom/                   # 분반 도메인 (예정)
│   ├── student/                     # 학생 도메인 (예정)
│   ├── subject/                     # 과목 도메인 (예정)
│   ├── lesson/                      # 수업 도메인 (예정)
│   └── request/                     # 요청 도메인 (예정)
│
└── SonmoumApiApplication.java
```

### 3.4 이벤트 기반 도메인 통신

도메인 간 강결합을 방지하기 위해 Spring ApplicationEvent를 활용합니다.

#### 이벤트 흐름 예시: 과목 생성 시 수업 자동 생성

```
Subject Domain                    Lesson Domain
     │                                 │
     │  SubjectCreatedEvent            │
     ├────────────────────────────────►│
     │                                 │
     │                         LessonService
     │                         - 수업 일정 계산
     │                         - Lesson 엔티티 생성
     │                                 │
     │                                 │  StudentAttendance 생성
     │                                 ├──────────────────────►
     │                                 │
```

#### 이벤트 구현

```java
// 이벤트 정의
public record SubjectCreatedEvent(
    Long subjectId,
    Long classroomId,
    Long teacherId,
    LocalDate startAt,
    LocalDate endAt,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    int times
) {}

// 이벤트 발행 (Subject Service)
@Service
@RequiredArgsConstructor
public class SubjectService {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Subject createSubject(SubjectCreateRequest request) {
        Subject subject = subjectRepository.save(...);

        eventPublisher.publishEvent(new SubjectCreatedEvent(
            subject.getId(),
            subject.getClassroomId(),
            subject.getTeacherId(),
            subject.getStartAt(),
            subject.getEndAt(),
            subject.getDayOfWeek(),
            subject.getStartTime(),
            subject.getEndTime(),
            subject.getTimes()
        ));

        return subject;
    }
}

// 이벤트 수신 (Lesson Service)
@Service
@RequiredArgsConstructor
public class LessonEventHandler {
    private final LessonService lessonService;

    @EventListener
    @Transactional
    public void handleSubjectCreated(SubjectCreatedEvent event) {
        lessonService.createLessonsFromSubject(event);
    }
}
```

### 3.5 주요 이벤트 목록

| 이벤트 | 발행 도메인 | 수신 도메인 | 설명 |
|--------|-------------|-------------|------|
| SubjectCreatedEvent | Subject | Lesson | 과목 생성 시 수업 자동 생성 |
| LessonCreatedEvent | Lesson | Student (Attendance) | 수업 생성 시 출석 레코드 생성 |
| AbsenceApprovedEvent | Request | Lesson | 결석 승인 시 출석 상태 변경 |
| ExchangeApprovedEvent | Request | Lesson/Subject | 교환 승인 시 담당자 변경 |
| DepartmentPermissionsGrantedEvent | Department | User | 부서 권한 추가 시 소속원들에게 권한 부여 (SYSTEM 타입) |
| DepartmentPermissionsRevokedEvent | Department | User | 부서 권한 삭제 시 소속원들에게 권한 회수 (SYSTEM 타입) |

---

## 4. 인증/인가

### 4.1 인증 방식

- **세션 기반 인증** (Spring Security `SessionCreationPolicy.IF_REQUIRED`)
- `CustomUserDetails`를 통해 `userId`, `email`, `authorities` 제공
- OAuth2 소셜 로그인 지원 (Google)

### 4.2 권한 체계 & Granter Tracking

**문제 해결**: 시스템(부서)에 의해 부여된 권한삭제 시, 관리자가 수동으로 부여한 권한까지 삭제되는 것을 방지하기 위해 **PermissionGranterType**을 도입했습니다.

- `PermissionGranterType.USER`: 관리자가 수동으로 부여한 권한
- `PermissionGranterType.SYSTEM`: 부서 변경 등 시스템 이벤트로 부여된 권한

> 사용자 권한은 `(userId, permissionId)` 쌍으로 유일성이 보장되지만, `granterType` 메타데이터를 통해 이 권한의 출처를 추적합니다. (현재 구현은 단순화를 위해 중복 저장은 하지 않고 메타데이터만 관리)

### 4.2 권한 체계

```java
public enum Role {
    VOLUNTEER,  // 봉사자
    ADMIN       // 관리자
}
```

### 4.3 권한 검증

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public List<UserResponse> getAllUsers() { ... }

@PreAuthorize("hasRole('VOLUNTEER') or hasRole('ADMIN')")
@GetMapping("/lessons/my")
public List<LessonResponse> getMyLessons() { ... }
```

---

## 5. API 설계 원칙

### 5.1 RESTful 규칙

- 리소스는 복수형 명사 사용 (`/users`, `/lessons`)
- HTTP 메서드로 행위 표현 (GET, POST, PUT, DELETE)
- 중첩 리소스는 2단계까지 (`/subjects/{id}/lessons`)

### 5.2 응답 형식

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다."
  }
}
```

### 5.3 페이지네이션

```
GET /api/v1/lessons?page=0&size=20&sort=date,desc
```

---

## 6. 데이터베이스

### 6.1 네이밍 규칙

- 테이블명: snake_case, 복수형 (`users`, `lessons`)
- 컬럼명: snake_case (`created_at`, `teacher_id`)
- 인덱스명: `idx_{테이블}_{컬럼}` (`idx_lessons_date`)

### 6.2 공통 컬럼

모든 엔티티는 `BaseEntity`를 상속:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

---

## 7. 테스트 전략

### 7.1 테스트 레벨

| 레벨 | 범위 | 도구 |
|------|------|------|
| Unit | Service, Entity | JUnit 5, Mockito |
| Integration | Repository, Controller | @SpringBootTest, @DataJpaTest |
| API | 전체 API | MockMvc, RestAssured |

### 7.2 테스트 패키지 구조

```
src/test/java/org/geumjeong/learning/
├── domain/
│   ├── user/
│   │   ├── service/UserServiceTest.java
│   │   └── controller/UserControllerTest.java
│   └── lesson/
│       └── ...
└── integration/
    └── LessonIntegrationTest.java
```

---

## 8. 배포 환경

### 8.1 프로필 구성

| 프로필 | 용도 | DB |
|--------|------|-----|
| `local` | 로컬 개발 | H2 (in-memory) |
| `dev` | 개발 서버 | PostgreSQL |
| `prod` | 운영 서버 | PostgreSQL |

### 8.2 환경 변수

```properties
# .env
DATABASE_URL=jdbc:postgresql://localhost:5432/sonmoum
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=password
GOOGLE_CLIENT_ID=xxx
GOOGLE_CLIENT_SECRET=xxx
```

---

## 9. 개발 컨벤션

> 상세 컨벤션은 [convention 폴더](./convention/)를 참조하세요.

- [커밋/브랜치/코드 스타일](./convention/convention.md)
- [PR 템플릿](./convention/pr_template.md)
- [Issue 템플릿 - Feature](./convention/issue_feature_template.md)
- [Issue 템플릿 - Bug](./convention/issue_bug_template.md)
