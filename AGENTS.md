# 금정야학 플랫폼 (GJLearn API)

금정열린배움터 교육 봉사 관리 시스템 백엔드 API

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
| Security | Spring Security + JWT | - |
| DB (prod) | PostgreSQL | - |
| DB (dev) | H2 | - |
| API Docs | SpringDoc OpenAPI | 2.8.x |

## 아키텍처

**모듈화된 도메인 기반 레이어드 아키텍처** + **이벤트 기반 도메인 통신**

### 패키지 구조

```
src/main/java/geumjeongyahak/
├── common/                     # 공통 모듈
│   ├── advice/                 # 전역 예외 처리
│   ├── config/                 # SwaggerConfig 등
│   ├── event/                  # 공통 이벤트 정의
│   ├── exception/              # 공통 예외, CommonErrorCode
│   ├── mail/                   # 메일 발송
│   ├── security/               # Security 설정
│   │   ├── config/             # SecurityProperties, WebSecurityConfig
│   │   ├── handler/            # Success/Failure Handler
│   │   ├── jwt/                # JwtTokenProvider, JwtAuthenticationFilter
│   │   └── service/            # CustomUserDetails, PermissionCodeEvaluator,
│   │                           #   DomainPermissionChecker
│   ├── util/
│   └── validation/             # 커스텀 Validation
│       ├── annotation/         # @ValidEmail 등
│       └── validator/          # Validator 구현체
│
└── domain/                     # 도메인 레이어 (24개)
    ├── base/                   # 공통 Base + 권한 레지스트리
    │   ├── entity/             # BaseEntity
    │   ├── enums/              # ResourceType, ActionType, PermissionScope
    │   ├── model/              # PermissionCode, PermissionRegistry
    │   └── dto/                # BasePageRequest/Response
    │
    ├── auth/                   # 인증 도메인
    │   ├── entity/             # RefreshToken, UserCredential
    │   ├── enums/              # RoleType, ProviderType
    │   ├── external/           # GoogleApiClient
    │   ├── service/            # LocalAuthService, GoogleAuthService,
    │   │                       #   RefreshTokenService 등
    │   └── v1/{controller,dto} # LocalAuthController, GoogleAuthController 등
    │
    ├── users/                  # 사용자 도메인
    │   ├── entity/             # User, UserPermission
    │   ├── repository/         # UserRepository, UserPermissionRepository
    │   │                       #   (+ specification/UserSpecs)
    │   ├── service/            # UserCrudService, UserPermissionService,
    │   │                       #   UserProxyService, TeacherService 등
    │   └── v1/{controller,dto} # UserAdminController, UserSelfController,
    │                           #   UserPermissionController, TeacherController
    │
    ├── request/                # 결석·교환 요청      ├── purchase_request/  결제·품의·결의
    ├── daily_schedule/         # 일정·수업일지       ├── teacher_assignment/ 교사 배정
    ├── lesson/  subject/  student/  classroom/  department/  organization/
    ├── channel/ post/  comment/  file/  event/  meeting_record/  notification/
    └── sitecontent/  teacher_application/  vendor/  actuator/
```

### 도메인별 패키지 구조

```
domain/{도메인명}/
├── entity/        # @Entity
├── repository/    # @Repository
├── service/       # @Service
├── (exception/)   # 도메인 예외 (필요시)
├── (enums/)       # Enum (필요시)
├── (event/)       # 도메인 이벤트 (필요시)
└── v1/            # API v1
    ├── controller/
    └── dto/
        ├── request/
        └── response/
```

## 핵심 패턴

### 1. 도메인 간 통신

통로가 둘이다. **무엇을 하느냐로 갈린다.**

**조회는 `*ProxyService`** — 도메인이 좁은 읽기 전용 API를 내놓고 다른 도메인이 그것을
주입한다. 다른 도메인의 Repository나 내부 Service를 직접 주입하지 않는다.

```java
// domain/users/service/UserProxyService.java — users 도메인이 내놓는 창구
@Transactional(readOnly = true)
public boolean existsById(Long userId) { ... }

// 다른 도메인은 이것만 주입한다
private final UserProxyService userProxyService;
```

**상태 변화·부수 효과는 이벤트** — Spring ApplicationEvent.

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

### 2. 응답 형식

컨트롤러는 `ResponseEntity<XxxResponse>`를 반환한다. 본문이 없으면
`ResponseEntity<Void>`다. **공통 래퍼 클래스는 쓰지 않는다.**

```java
@GetMapping("/{id}")
public ResponseEntity<PurchaseRequestDetailResponse> getDetail(@PathVariable Long id) {
    return ResponseEntity.ok(service.getDetail(id));
}
```

응답 DTO는 도메인의 `v1/dto/response/`에 두고 record로 만든다. **Entity를 그대로
응답에 싣지 않는다.**

에러는 `common/advice`의 전역 예외 처리기가 도메인별 `*ErrorCode`를 받아 변환한다.

### 3. BaseEntity 상속

엔티티는 `BaseEntity`를 상속한다 (createdAt, updatedAt 자동 관리)

```java
@Entity
public class User extends BaseEntity { ... }
```

기존 예외 넷은 상속하지 않는다 — `RefreshToken`, `File`, `PurchaseRequestItem`,
`PurchaseRequestPaymentTransaction`. 새 엔티티는 상속한다.

## 코드 컨벤션

### 커밋 메시지 (Conventional Commits)

```
<type>(<scope>): <subject>
```

**Type**: feat, fix, docs, style, refactor, test, chore

**Scope**: 도메인 폴더 이름을 하이픈으로 바꿔 쓴다 (`purchase_request` → `purchase-request`)

최근 80커밋 사용 빈도: `request`(19) · `purchase-request`(13) · `user`(6) ·
`global`(6) · `daily-schedule`(5) · `subject`(4) · `lesson`(4) · `auth`(3) · `seed`(2)

**예시**:
```
feat(lesson): 수업 캘린더 조회 API 추가
fix(auth): 로그인 시 세션 만료 오류 수정
```

### 브랜치 전략

```
main
 └── dev
      ├── feat/{issue-number}-{slug}     [FEAT] 이슈
      ├── fix/{issue-number}-{slug}      [FIX] 이슈
      └── docs/{issue-number}-{slug}

main
 └── hotfix/{slug}                       긴급 프로덕션 수정만
```

통합 브랜치는 `dev`다. 기능 브랜치 접두사는 **`feat/`**이고 `feature/`가 아니다
(오래된 브랜치에 `feature/`가 남아 있다).

### Issue · PR 형식

- 이슈 제목은 `[FEAT] …` 또는 `[FIX] …`, 라벨은 `enhancement` / `bug`
- 빈 이슈는 못 만든다 (`blank_issues_enabled: false`). 템플릿 절을 그대로 지킨다
- **PR 제목은 이슈 제목을 그대로 쓴다**
- PR base는 `dev`. PR에 라벨은 안 단다
- PR 본문 절 일곱: 개요 · 변경 유형 · 변경 내용 · 관련 이슈 · 스크린샷 (선택) ·
  체크리스트 · 리뷰어에게

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

층이 둘이다. **역할**은 사람이 무엇인지고, **세밀 권한**은 무엇을 할 수 있는지다.

### 1. 역할 — `RoleType`

```java
public enum RoleType {
    ADMIN, MANAGER, VOLUNTEER, GUEST;

    public GrantedAuthority getAuthority() {
        return new SimpleGrantedAuthority("ROLE_" + name());
    }
}
```

넷뿐이고 전부 `ROLE_` 접두사가 붙는다. **사용자는 역할을 하나만 갖는다** —
`User.role` 단일 필드이고 `users.role` 컬럼이다. 여러 역할을 붙이는 조인 테이블은 없다.

```java
@PreAuthorize("hasRole('ADMIN')")
```

### 2. 세밀 권한 — `PermissionCode`

`domain/base/model/PermissionCode`가 `resource:action:target` 형태의 값 객체다.

| 자리 | 무엇 | 값 |
|---|---|---|
| `resource` | `ResourceType` | channel · subject · student · department · lesson · daily_schedule · user · absence_request · purchase_request · vendor · event · teacher_application · lesson_exchange_request |
| `action` | `ActionType` | read · write · grant · manage · review |
| `target` | 대상 | `*` (global) 또는 양의 정수 ID |

```java
@PreAuthorize("hasAuthority('user:manage:*')")
```

등록되지 않은 `resource`·`action` 조합은 `PermissionRegistry.validate`가 거절한다.
평가는 `common/security/service`의 `PermissionCodeEvaluator`·`DomainPermissionChecker`가 한다.

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
- [API Spec](docs/api-spec/api_spec.md) - API 흐름 (시퀀스 다이어그램)
- [Data Model](docs/data_model.md) - 데이터 모델
- [Convention](docs/convention/) - 개발 컨벤션
