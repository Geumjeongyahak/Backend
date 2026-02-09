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
| JWT | JJWT | Access/Refresh Token 관리 |
| Password | BCrypt | 비밀번호 암호화 |

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
src/main/java/sonmoeum/
├── common/                          # 공통 모듈
│   ├── advice/                      # 전역 예외 처리
│   │   └── GlobalExceptionHandler.java
│   ├── config/                      # 설정 클래스
│   │   └── SwaggerConfig.java
│   ├── event/                       # 이벤트 정의
│   ├── exception/                   # 공통 예외
│   ├── security/                    # Security 관련
│   │   ├── config/
│   │   │   ├── SecurityProperties.java
│   │   │   └── WebSecurityConfig.java
│   │   ├── filter/                  # Security Filter
│   │   │   └── JwtAuthenticationFilter.java
│   │   ├── handler/                 # Success/Failure Handler
│   │   ├── jwt/                     # JWT 토큰 처리
│   │   │   └── JwtTokenProvider.java
│   │   └── service/                 # UserDetails
│   │       └── CustomUserDetails.java
│   └── validation/                  # 커스텀 Validation
│       ├── annotation/              # @ValidEmail 등
│       └── validator/               # Validator 구현체
│
└── domain/                          # 도메인 레이어
    ├── base/                        # 공통 Base 클래스
    │   ├── dto/                     # BasePageRequest, BasePageResponse
    │   └── entity/
    │       └── BaseEntity.java      # created_at, updated_at
    │
    ├── auth/                        # 인증/권한 도메인
    │   ├── entity/
    │   │   ├── Role.java
    │   │   └── RefreshToken.java
    │   ├── enums/
    │   │   └── RoleType.java        # ADMIN, MANAGER, VOLUNTEER 등
    │   ├── repository/
    │   │   └── RefreshTokenRepository.java
    │   ├── service/
    │   │   ├── LocalLoginService.java
    │   │   ├── PasswordService.java
    │   │   ├── DuplicateCheckService.java
    │   │   ├── AuthService.java
    │   │   └── RefreshTokenService.java
    │   └── v1/                      # API v1
    │       ├── controller/
    │       │   ├── AuthController.java
    │       │   ├── PasswordController.java
    │       │   ├── DuplicateCheckController.java
    │       │   └── MeController.java
    │       └── dto/
    │           ├── request/         # LocalLoginRequest, LocalSignupRequest 등
    │           └── response/        # LoginResponse, TokenResponse 등
    │
    ├── users/                       # 사용자 도메인
    │   ├── entity/
    │   │   ├── User.java
    │   │   └── UserRole.java
    │   ├── exception/
    │   │   ├── UserNotFoundException.java
    │   │   └── DuplicateUsernameException.java
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   └── UserRoleRepository.java
    │   ├── service/
    │   │   ├── UserAdminService.java
    │   │   ├── UserRoleService.java
    │   │   └── UserProxyService.java
    │   └── v1/                      # API v1
    │       ├── controller/
    │       │   ├── UserController.java
    │       │   └── UserRoleController.java
    │       └── dto/
    │           ├── request/         # CreateUserRequest, UpdateUserRequest 등
    │           └── response/        # UserResponse
    │
    ├── classroom/                   # 분반 도메인
    │   ├── entity/
    │   ├── repository/
    │   └── v1/
    │
    ├── student/                     # 학생 도메인
    │   ├── entity/
    │   ├── repository/
    │   └── v1/
    │
    ├── subject/                     # 과목 도메인
    │   ├── entity/
    │   ├── repository/
    │   ├── event/                   # SubjectCreatedEvent
    │   └── v1/
    │
    ├── lesson/                      # 수업 도메인
    │   ├── entity/
    │   ├── repository/
    │   └── v1/
    │
    └── request/                     # 요청 도메인
        ├── entity/
        ├── repository/
        └── v1/
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

---

## 4. 인증/인가

### 4.1 인증 방식

- **JWT 기반 인증** (Spring Security `SessionCreationPolicy.STATELESS`)
- **Access Token + Refresh Token** 구조
  - Access Token: 짧은 만료 시간 (기본 1시간), API 요청 시 사용
  - Refresh Token: 긴 만료 시간 (기본 14일), Access Token 재발급용
- `CustomUserDetails`를 통해 `userId`, `username`, `authorities` 제공
- Refresh Token Rotation 적용 (재발급 시 기존 토큰 무효화)

### 4.2 역할 기반 권한 체계

**RoleType Enum**을 통한 역할 관리:

```java
public enum RoleType {
    // 기본 역할 (level 0)
    ADMIN(1L),           // 관리자 - 시스템 전체 관리
    MANAGER(2L),         // 매니저 - 중간 관리자
    VOLUNTEER(3L),       // 봉사자 - 수업 진행
    GUEST(4L),           // 게스트 - 제한적 접근

    // 부서 역할 (level 1-999)
    DEPT_FINANCE(1001L),   // 재정 부서
    DEPT_ACADEMIC(1002L),  // 학사 부서
    DEPT_IT(1003L),        // IT 부서
    DEPT_SUPPORT(1004L),   // 지원 부서

    // 교육 역할 (level 1000+)
    TEACHER(2001L);        // 교사

    public String getAuthority() {
        // level % 1000 == 0 → "ROLE_" prefix 추가
        // level % 1000 != 0 → prefix 없음
        if (this.level == 0) return "ROLE_" + this.name();
        return this.name();
    }
}
```

**특징:**
- 한 사용자는 여러 역할을 동시에 가질 수 있음 (N:M 관계)
- 역할별 권한은 Spring Security의 `hasRole()`, `hasAuthority()`로 검증
- 기본 역할은 `ROLE_` prefix, 부서/교육 역할은 prefix 없음

### 4.3 권한 검증

```java
// ADMIN 역할만 접근 가능
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/api/v1/users")
public List<UserResponse> getAllUsers() { ... }

// VOLUNTEER 또는 ADMIN 역할 접근 가능
@PreAuthorize("hasRole('VOLUNTEER') or hasRole('ADMIN')")
@GetMapping("/api/v1/lessons/my")
public List<LessonResponse> getMyLessons() { ... }

// 재정 부서 역할 필요
@PreAuthorize("hasAuthority('DEPT_FINANCE')")
@GetMapping("/api/v1/finance/reports")
public FinanceReport getReport() { ... }

// 복합 권한 (ADMIN이거나 TEACHER 역할)
@PreAuthorize("hasRole('ADMIN') or hasAuthority('TEACHER')")
@PostMapping("/api/v1/lessons/{id}/reviews")
public LessonReview createReview() { ... }
```

### 4.4 Refresh Token 관리

- Repository 기반 저장 (PostgreSQL/H2)
- 사용자당 하나의 Refresh Token만 유지 (새 토큰 발급 시 기존 토큰 삭제)
- 만료된 토큰 자동 정리 기능 제공
- 로그아웃 시 해당 사용자의 Refresh Token 무효화

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
