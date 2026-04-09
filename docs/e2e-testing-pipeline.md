# E2E 테스트 작성 파이프라인

손모음 플랫폼 백엔드의 E2E 테스트를 일관된 방법으로 작성하기 위한 가이드.

---

## 1. 테스트 실행 방법

```bash
# 전체 E2E 테스트
./gradlew test -Dgroups="e2e"

# 특정 도메인만
./gradlew test -Dgroups="absence-request"
./gradlew test -Dgroups="lesson-exchange-request"
./gradlew test -Dgroups="purchase-request"
./gradlew test -Dgroups="subject-exchange-request"
./gradlew test -Dgroups="lesson"
./gradlew test -Dgroups="request"        # 요청 도메인 전체

# 여러 태그 조합
./gradlew test -Dgroups="absence-request | lesson-exchange-request"
```

---

## 2. 디렉터리 구조

```
src/test/java/sonmoeum/
├── e2e/
│   ├── BaseE2ETest.java              ← 모든 E2E 테스트의 최상위 베이스
│   ├── util/
│   │   ├── TestUserHelper.java       ← 사용자 생성·토큰 발급
│   │   ├── TestClassroomHelper.java  ← 분반 생성
│   │   ├── TestDepartmentHelper.java ← 부서 생성·가입
│   │   └── TestLessonHelper.java     ← 과목·수업 생성 (보조 도메인)
│   ├── {domain}/
│   │   ├── {Domain}BaseTest.java     ← 도메인별 공통 설정
│   │   ├── {Domain}CreateTest.java
│   │   ├── {Domain}ReadTest.java
│   │   └── {Domain}StatusTest.java   ← 승인·반려·삭제 등 상태 변경
│   └── request/                      ← 요청 도메인 예시
│       ├── RequestBaseTest.java
│       ├── absence/
│       ├── lessonexchange/
│       ├── purchase/
│       └── subjectexchange/
```

---

## 3. 새 도메인 E2E 테스트 작성 파이프라인

### Step 1 – 사전 분석

테스트 작성 전에 다음을 파악한다.

| 항목 | 확인 방법 |
|------|----------|
| API 경로 및 HTTP 메서드 | `*Controller.java` `@RequestMapping` 확인 |
| 요청/응답 필드 | `*Request.java`, `*Response.java` record 확인 |
| 인증/권한 규칙 | 컨트롤러 `@PreAuthorize` 확인 |
| 비즈니스 제약 | `*Service.java` 로직 확인 (중복 검사, 상태 전이 등) |
| **Side effect** | 승인/상태 변경 시 **다른 도메인 엔티티가 변경되는지** 확인 |
| 보조 도메인 필요 여부 | 레슨 기반(AbsenceRequest) vs 과목 기반(PurchaseRequest) |

### Step 2 – 헬퍼 준비

보조 도메인이 필요하면 `src/test/java/sonmoeum/e2e/util/` 에 헬퍼를 추가한다.

```java
@Component
public class TestXxxHelper {
    // HTTP를 통해 생성 → ID 반환
    public Long createXxxAndGetId(String authHeader, ...) { ... }
    // cleanup용 삭제
    public void deleteXxx(String authHeader, Long id) { ... }
    // side-effect 검증용 조회
    public String getXxxField(String authHeader, Long id) { ... }
}
```

**원칙:**
- 헬퍼는 `@Component`로 스프링 빈으로 등록
- 충돌 방지를 위해 `AtomicInteger` 또는 `timestamp`로 유일성 보장
- 헬퍼는 상태를 갖지 않음 (stateless) – 각 테스트가 ID를 직접 추적

### Step 3 – BaseTest 작성

`RequestBaseTest` 패턴을 따른다.

```java
@Tag("domain-name")           // JUnit5 태그 (소문자 kebab-case)
public abstract class XxxBaseTest extends BaseE2ETest {

    // init_data 고정 ID 상수 선언
    protected static final long SOME_FIXED_ID = 1L;

    @Autowired
    protected TestXxxHelper xxxHelper;   // 필요한 헬퍼만 주입

    protected String adminToken;
    protected String volunteerToken;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        adminToken = userTestHelper.generateAccessToken(TEST_ADMIN_USERNAME);
        volunteerToken = userTestHelper.generateAccessToken("teacher01");
    }

    // 공통 생성 헬퍼 (201 검증 + id 반환)
    protected Long createXxx(String authHeader, ...) { ... }
}
```

### Step 4 – 테스트 케이스 설계 체크리스트

각 테스트 파일이 커버해야 할 케이스:

#### Create 테스트

- [ ] 성공 (정상 사용자, 필드 값 검증)
- [ ] 성공 (다른 권한 사용자)
- [ ] 401 – 인증 없음
- [ ] 403 – 권한 부족 (필요한 경우)
- [ ] 404 – 연관 엔티티 존재하지 않음
- [ ] 400 – 필수 필드 null/blank
- [ ] 400 – 값 범위 위반 (@Min, @Max 등)
- [ ] 409 – 중복 (비즈니스 중복 검사가 있는 경우)

#### Read 테스트

- [ ] 200 – 목록 조회 (관리자 전체 조회)
- [ ] 200 – 목록 조회 (비관리자 → 본인 요청만 포함, 타인 것 미포함)
- [ ] 200 – status 필터 적용
- [ ] 200 – 단건 조회 (소유자)
- [ ] 200 – 단건 조회 (관리자)
- [ ] 403 – 단건 조회 (타인)
- [ ] 404 – 단건 조회 (존재하지 않는 ID)
- [ ] 401 – 인증 없음

#### Status 테스트 (approve/reject/delete)

- [ ] 200 – 승인 성공 (상태, 필드 검증)
- [ ] **Side-effect 검증** – 승인 시 연관 엔티티 변경 확인
- [ ] 409 – 이미 승인된 요청 재승인
- [ ] 409 – 반려 후 승인 시도 (또는 역방향)
- [ ] 403 – 권한 없는 사용자 승인 시도
- [ ] 404 – 존재하지 않는 요청 승인
- [ ] 401 – 인증 없음
- [ ] 200 – 반려 성공 (note 필드 검증)
- [ ] 400 – note 없이 반려
- [ ] 400 – note 빈 문자열로 반려
- [ ] 409 – 이미 처리된 요청 반려
- [ ] 403 – 권한 없는 사용자 반려
- [ ] 204 – 삭제 성공 (삭제 엔드포인트가 있는 경우)
- [ ] 403 – 타인 요청 삭제 시도
- [ ] 404 – 존재하지 않는 요청 삭제

### Step 5 – 테스트 격리 전략

#### 핵심 원칙
> **각 테스트 메서드는 자신이 생성한 데이터에만 의존해야 한다.**

| 상황 | 전략 |
|------|------|
| 승인 시 연관 엔티티 변경 (side-effect) | **독립 엔티티 생성** – `@BeforeEach` 또는 테스트 내에서 직접 생성 |
| 과목/수업 기반 요청 | `TestLessonHelper`로 테스트별 독립 수업 생성 |
| 과목 상태 미변경 요청 (Purchase, SubjectExchange) | init_data `SUBJECT_ID=1` 재사용 가능 |
| 중복 체크가 있는 생성 | 테스트마다 고유 식별자 사용 |

#### cleanup 패턴

```java
// 필드 선언
private Long createdEntityId;
private Long createdRequestId;

// @AfterEach – 역순으로 삭제 (의존성 순서 고려)
@AfterEach
void cleanup() {
    if (createdRequestId != null) {
        requestRepository.deleteById(createdRequestId);  // 요청 먼저
        createdRequestId = null;
    }
    if (createdEntityId != null) {
        helper.deleteEntity(getAuthHeader(adminToken), createdEntityId);  // 연관 엔티티 나중에
        createdEntityId = null;
    }
}
```

**삭제 방법 우선순위:**
1. HTTP DELETE 엔드포인트 (있는 경우, API 동작도 검증됨)
2. `@Autowired` Repository 직접 삭제 (DELETE 엔드포인트 없는 경우)

#### 여러 요청을 한 테스트에서 다루는 경우

```java
@Test
void getList_adminSeesAll_volunteerSeesOnlyOwn() {
    Long req1 = createRequest(volunteer1Token, ...);
    Long req2 = createRequest(volunteer2Token, ...);

    try {
        // 검증 로직
    } finally {
        // 무조건 cleanup
        repository.deleteById(req1);
        repository.deleteById(req2);
    }
}
```

### Step 6 – Tag 전략

```
@Tag("e2e")              ← BaseE2ETest (모든 E2E에 자동 부여)
@Tag("request")          ← RequestBaseTest (요청 도메인 그룹)
@Tag("absence-request")  ← 결석 요청 구체 테스트 클래스
@Tag("lesson")           ← 수업 도메인
```

규칙:
- 베이스 클래스에 그룹 태그 → 하위 클래스가 자동 상속
- 도메인 태그는 `kebab-case` 소문자
- 실행 시: `./gradlew test -Dgroups="태그명"`

---

## 4. init_data 고정 ID 참조 표

| 구분 | ID | 설명 |
|------|----|------|
| User admin1234 | 1 | ROLE_ADMIN |
| User teacher01 | 2 | ROLE_VOLUNTEER (홍길동) |
| User teacher02 | 3 | ROLE_VOLUNTEER (김철수) |
| Classroom 벚꽃반 | 1 | WEEKDAY |
| Subject 한글 기초 | 1 | classroom=1, teacher=2, FRIDAY |
| Subject 수학 기초 | 2 | classroom=2, teacher=3, FRIDAY |
| Subject 스마트폰 활용 | 3 | classroom=3, teacher=2, SATURDAY |
| Lesson | 1 | subject=1, teacher=2, 2026-02-13 |
| Lesson | 2 | subject=2, teacher=3, 2026-02-13 |
| Lesson | 3 | subject=3, teacher=2, 2026-02-14 |

**주의:** 레슨 기반 요청(AbsenceRequest, LessonExchangeRequest)은 승인 시 레슨 상태가 변경되므로
init_data 레슨(1~3)을 직접 사용하면 안 된다. `TestLessonHelper`로 테스트별 독립 레슨을 생성한다.

---

## 5. Side-effect 검증 패턴

승인/처리 후 다른 도메인 엔티티가 변경되는 케이스는 별도 테스트로 분리한다.

```java
@Test
@DisplayName("[Side-effect] 결석 요청 승인 → 수업 teacherAttendance 가 EXCUSED 로 변경")
void approve_updatesLessonTeacherAttendance_toExcused() {
    // Arrange
    currentRequestId = setupPendingRequest();

    // Act: 승인 전 상태 확인
    String before = lessonHelper.getLessonTeacherAttendance(adminToken, lessonId);
    assertThat(before).isEqualTo("ABSENT");

    given().patch("/{id}/approve", currentRequestId).then().statusCode(200);

    // Assert: side-effect 확인
    String after = lessonHelper.getLessonTeacherAttendance(adminToken, lessonId);
    assertThat(after).isEqualTo("EXCUSED");
}
```

현재 시스템의 Side-effect 목록:

| 트리거 | 변경 엔티티 | 변경 필드 |
|--------|-----------|---------|
| AbsenceRequest 승인 | Lesson | `teacherAttendance` → `EXCUSED` |
| LessonExchangeRequest 승인 | Lesson | `teacher` → `exchangeWithUserId` 사용자 |

---

## 6. 자주 쓰는 RestAssured 패턴

```java
// 생성 (201)
Long id = given()
    .basePath("/api/v1/xxx")
    .header(AUTH_HEADER, getAuthHeader(adminToken))
    .contentType(ContentType.JSON)
    .body(Map.of("field", "value"))
    .post()
    .then()
    .statusCode(201)
    .body("status", equalTo("PENDING"))
    .extract().jsonPath().getLong("id");

// 목록 조회 후 특정 ID 포함 여부 검증
List<Long> ids = given()
    .basePath("/api/v1/xxx")
    .header(AUTH_HEADER, getAuthHeader(adminToken))
    .queryParam("status", "PENDING")
    .get()
    .then().statusCode(200)
    .extract().jsonPath().getList("id", Long.class);
assertThat(ids).contains(expectedId);
assertThat(ids).doesNotContain(unexpectedId);

// basePath 주의: given() 에 명시하면 전역 설정 무시
// ✅ 명시적 basePath 사용 권장
given().basePath("/api/v1/subjects").post()    // full path로 POST
// ❌ 전역 basePath 에 의존하면 helper 호출 시 꼬일 수 있음
```
