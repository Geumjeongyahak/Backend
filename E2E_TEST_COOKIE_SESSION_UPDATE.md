# E2E 테스트 쿠키 세션 적용 완료

## 적용 내용

### 1. 쿠키 세션 방식 확인 ✅
- Spring Session을 사용하여 **`SESSION`** 쿠키로 인증 관리
- `JSESSIONID` → `SESSION` 쿠키로 변경

### 2. 권한 기반 인증 시스템 적용 ✅

#### 권한 체계
```java
@PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_USERS')")
```

**PermissionType (8개)**:
- `SUPER_ADMIN` (100L) - 슈퍼 관리자
- `MANAGE_USERS` (200L) - 사용자 관리
- `MANAGE_DEPARTMENTS` (300L) - 부서 관리
- `MANAGE_CLASSROOMS` (400L) - 분반 관리
- `MANAGE_STUDENTS` (500L) - 학생 관리
- `MANAGE_SUBJECTS` (600L) - 과목 관리
- `MANAGE_LESSONS` (700L) - 수업 관리
- `MANAGE_REQUESTS` (800L) - 요청 관리

#### 권한 부여 방식
```java
// UserPermission 생성
UserPermission userPermission = new UserPermission(
    user,
    PermissionType.SUPER_ADMIN,
    PermissionGranterType.USER
);
userPermissionRepository.save(userPermission);
```

### 3. AbstractE2ETest 개선 ✅

#### 추가된 헬퍼 메서드

```java
/**
 * 관리자 사용자 생성 (SUPER_ADMIN 권한 포함)
 */
protected User createAdminUser(String email, String password) { ... }

/**
 * 특정 권한을 가진 사용자 생성
 */
protected User createUserWithPermissions(String email, String password, PermissionType... permissions) { ... }

/**
 * 사용자에게 권한 부여
 */
protected void grantPermission(User user, PermissionType permissionType) { ... }

/**
 * SESSION 쿠키로 로그인
 */
protected String login(String email, String password) {
    io.restassured.response.Response response = given()
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(Map.of("email", email, "password", password))
    .when()
        .post("/api/v1/auth/login")
    .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .response();

    String sessionCookie = response.getCookie("SESSION");
    if (sessionCookie == null) {
        throw new IllegalStateException("로그인 후 SESSION 쿠키를 찾을 수 없습니다.");
    }
    return sessionCookie;
}
```

#### 쿠키 사용 방법

```java
// 로그인하여 세션 쿠키 얻기
String sessionCookie = loginAsAdmin();

// API 호출 시 쿠키 사용
given()
    .cookie("SESSION", sessionCookie)
    .contentType(MediaType.APPLICATION_JSON_VALUE)
    .body(requestBody)
.when()
    .post("/api/v1/users")
.then()
    .statusCode(HttpStatus.CREATED.value());
```

### 4. 전체 테스트 일괄 변환 ✅

모든 E2E 테스트 파일에서 다음 변경사항 적용:
- `String sessionId` → `String sessionCookie`
- `.sessionId(sessionId)` → `.cookie("SESSION", sessionCookie)`
- `JSESSIONID` → `SESSION`

## 테스트 결과

### 이전 상태
```
50 tests completed, 48 failed
성공률: 4% (2/50)
```

### 현재 상태 (쿠키 세션 적용 후)
```
50 tests completed, 35 failed
성공률: 30% (15/50)
```

**개선율: 650% (2개 → 15개 성공)**

### 성공한 테스트 예시
- ✅ 로그인 성공
- ✅ 로그인 실패 - 잘못된 이메일 형식
- ✅ 로그아웃 성공
- ✅ 내 정보 조회 성공
- ✅ 사용자 생성 성공
- ✅ 사용자 조회 성공
- ✅ 사용자 수정 성공
- ✅ 사용자 삭제 성공
- ✅ 부서 생성 성공
- ✅ 학생 생성 성공
- ✅ 과목 생성 성공
- ✅ 요청 생성 성공
- 등등...

### 여전히 실패하는 테스트
대부분 **500 Internal Server Error** 발생:
- 로그인 실패 케이스 (잘못된 비밀번호, 존재하지 않는 사용자)
- 일부 권한 검증 테스트

## 남은 작업

### 1. 500 에러 원인 파악 및 수정
실패 테스트에서 예상 상태 코드(401, 403, 404)가 아닌 500이 반환되는 경우가 있습니다.

**디버깅 방법**:
```bash
# 특정 테스트 상세 로그 확인
./gradlew test --tests "LoginE2ETest.loginFailWrongPassword" --info

# 서버 로그 확인
tail -f build/test-results/test/*.xml
```

**가능한 원인**:
1. 인증 실패 시 예외 처리가 제대로 되지 않음
2. 권한 체크 로직에서 NullPointerException 발생
3. UserDetailsService 구현 문제

### 2. 응답 구조 검증

일부 테스트에서 `data.field` 경로가 실제 응답과 다를 수 있습니다.

**확인 방법**:
```java
given()
    .cookie("SESSION", sessionCookie)
.when()
    .get("/api/v1/users/1")
.then()
    .log().all() // 전체 응답 출력
    .statusCode(HttpStatus.OK.value());
```

### 3. 권한 검증 강화

권한이 없을 때 403이 제대로 반환되는지 확인:

```java
@Test
@DisplayName("사용자 생성 실패 - 권한 없음")
void createUserFailUnauthorized() {
    // 권한 없는 봉사자로 로그인
    String sessionCookie = loginAsVolunteer();

    var userRequest = createUserRequest("test@example.com", "password", "Test");

    given()
        .cookie("SESSION", sessionCookie)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(userRequest)
    .when()
        .post("/api/v1/users")
    .then()
        .statusCode(HttpStatus.FORBIDDEN.value()); // 403
}
```

## 핵심 패턴 정리

### 패턴 1: 관리자 인증 필요 API
```java
@Test
@DisplayName("리소스 생성 성공")
void createResourceSuccess() {
    // 관리자 로그인 (SUPER_ADMIN 권한 포함)
    String sessionCookie = loginAsAdmin();

    var request = createRequest(...);

    given()
        .cookie("SESSION", sessionCookie)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(request)
    .when()
        .post("/api/v1/resources")
    .then()
        .statusCode(HttpStatus.CREATED.value())
        .body("data.id", notNullValue());
}
```

### 패턴 2: 인증만 필요한 API (권한 불필요)
```java
@Test
@DisplayName("내 정보 조회")
void getMyInfo() {
    // 일반 봉사자로 로그인
    String sessionCookie = loginAsVolunteer();

    given()
        .cookie("SESSION", sessionCookie)
    .when()
        .get("/api/v1/auth/me")
    .then()
        .statusCode(HttpStatus.OK.value());
}
```

### 패턴 3: 특정 권한을 가진 사용자 테스트
```java
@Test
@DisplayName("사용자 관리 권한으로 사용자 생성")
void createUserWithManageUsersPermission() {
    // MANAGE_USERS 권한만 가진 사용자 생성
    User user = createUserWithPermissions(
        "manager@example.com",
        "password",
        PermissionType.MANAGE_USERS
    );
    String sessionCookie = login("manager@example.com", "password");

    var request = createUserRequest(...);

    given()
        .cookie("SESSION", sessionCookie)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(request)
    .when()
        .post("/api/v1/users")
    .then()
        .statusCode(HttpStatus.CREATED.value());
}
```

## 실행 명령어

```bash
# 전체 E2E 테스트 실행
./gradlew test --tests "*E2E Test"

# 성공한 테스트만 확인
./gradlew test --tests "*E2ETest" | grep "tests completed"

# 특정 도메인 테스트
./gradlew test --tests "*.users.*E2ETest"

# 단일 테스트 디버깅
./gradlew test --tests "LoginE2ETest.loginSuccess" --info
```

## 다음 단계

1. **즉시**: 500 에러 원인 파악 및 서버 쪽 수정
   - GlobalExceptionHandler 확인
   - CustomUserDetailsService 확인
   - 인증/인가 필터 체인 확인

2. **단기**: 나머지 35개 실패 테스트 수정
   - 응답 구조 검증
   - 상태 코드 기대값 조정
   - 권한 부여 로직 수정

3. **중기**: 누락된 테스트 완성
   - Departments Update/Delete
   - Students Read/Update/Delete
   - Classrooms 전체 CRUD
   - Subjects Read/Update/Delete
   - Lessons UpdateAttendance
   - Requests Exchange 전체
   - Requests UpdateStatus 전체

4. **장기**: 고급 시나리오
   - 세션 만료 테스트
   - 동시성 테스트
   - 성능 테스트

## 요약

쿠키 세션 인증 방식을 성공적으로 적용하여 **E2E 테스트 성공률을 4%에서 30%로 향상**시켰습니다.

### 핵심 변경사항
1. ✅ `SESSION` 쿠키 기반 인증
2. ✅ Permission 기반 권한 관리
3. ✅ `createAdminUser()` 헬퍼 메서드 (SUPER_ADMIN 권한 자동 부여)
4. ✅ 모든 테스트 파일 일괄 변환

### 주요 성과
- **15개 테스트 성공** (기본 CRUD 및 인증 시나리오)
- 재사용 가능한 테스트 인프라 구축
- 명확한 권한 관리 패턴 정립

### 개선 필요
- 서버 쪽 예외 처리 개선 (500 에러 → 적절한 4xx 에러)
- 나머지 35개 테스트 디버깅 및 수정
- 누락된 테스트 케이스 완성

전반적으로 **견고한 E2E 테스트 프레임워크**가 구축되었으며, 남은 작업은 주로 서버 쪽 버그 수정과 테스트 케이스 완성입니다.
