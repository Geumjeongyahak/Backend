# E2E 테스트 완성 가이드

## 빠른 시작

### 1. 테스트 실패 원인 파악 및 수정

먼저 한 개의 간단한 테스트를 성공시키는 것이 목표입니다.

```bash
# 테스트 리포트 확인
./gradlew test --tests "LoginE2ETest.loginSuccess"
# 리포트: build/reports/tests/test/index.html
```

#### 가능한 문제와 해결 방법

**문제 1: 세션 ID를 추출할 수 없음**
```java
// 현재 방식
String sessionId = given()
    .contentType(MediaType.APPLICATION_JSON_VALUE)
    .body(Map.of("email", email, "password", password))
.when()
    .post("/api/v1/auth/login")
.then()
    .statusCode(HttpStatus.OK.value())
    .extract()
    .sessionId();

// 대안 1: 쿠키에서 직접 추출
String sessionId = given()
    .contentType(MediaType.APPLICATION_JSON_VALUE)
    .body(Map.of("email", email, "password", password))
.when()
    .post("/api/v1/auth/login")
.then()
    .statusCode(HttpStatus.OK.value())
    .extract()
    .cookie("JSESSIONID");

// 대안 2: 모든 요청에서 같은 spec 사용
RequestSpecification spec = given()
    .contentType(MediaType.APPLICATION_JSON_VALUE)
    .body(Map.of("email", email, "password", password));

spec.when()
    .post("/api/v1/auth/login")
.then()
    .statusCode(HttpStatus.OK.value());

// 이후 같은 spec 재사용
spec.when()
    .get("/api/v1/auth/me")
.then()
    .statusCode(HttpStatus.OK.value());
```

**문제 2: 응답 구조가 다름**
```java
// ApiResponse 구조 확인 필요
// 실제 응답이 다음과 같을 수 있음:
// { "success": true, "data": {...} }
// { "status": "OK", "body": {...} }
// { "email": "...", "name": "..." } // data wrapper 없음

// 해결: 실제 응답 확인 후 수정
given()
    .sessionId(sessionId)
.when()
    .get("/api/v1/users/1")
.then()
    .statusCode(HttpStatus.OK.value())
    .body("email", equalTo("test@example.com")); // data. 제거 시도
```

**문제 3: 권한이 없음**
```java
// User 생성 시 권한 추가
protected User createAdminUser(String email, String password) {
    User user = userRepository.save(User.emailUserBuilder()
            .name("Admin User")
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .role(RoleType.MANAGER)
            .phoneNumber("010-0000-0000")
            .permissions(List.of())  // ← 여기에 권한 추가
            .build());

    // Permission 엔티티 생성 및 할당
    Permission manageUsers = new Permission(...);
    permissionRepository.save(manageUsers);

    UserPermission userPermission = new UserPermission(user, manageUsers);
    userPermissionRepository.save(userPermission);

    return user;
}
```

### 2. 나머지 테스트 패턴 복사하여 완성

기존 패턴을 복사하여 빠르게 확장할 수 있습니다.

#### 예시: Departments Update 테스트 작성

**1단계**: Users의 UpdateUserE2ETest.java 복사
```bash
cp src/test/java/org/geumjeong/learning/sonmoum_api/e2e/users/UpdateUserE2ETest.java \
   src/test/java/org/geumjeong/learning/sonmoum_api/e2e/departments/UpdateDepartmentE2ETest.java
```

**2단계**: 내용 수정 (Find & Replace)
- `User` → `Department`
- `user` → `department`
- `/api/v1/users` → `/api/v1/departments`
- `BaseUserE2ETest` → `BaseDepartmentE2ETest`
- 테스트 Display Name 수정

**3단계**: 필드명 수정
```java
Map<String, Object> updateRequest = Map.of(
    "name", "Updated Name",
    "description", "Updated Description"  // User의 phoneNumber → Department의 description
);
```

#### 예시: Classrooms 전체 CRUD 작성

1. **Create**: Students의 CreateStudentE2ETest.java 복사 후 수정
2. **Read**: Departments의 ReadDepartmentE2ETest.java 복사 후 수정
3. **Update**: Users의 UpdateUserE2ETest.java 복사 후 수정
4. **Delete**: Users의 DeleteUserE2ETest.java 복사 후 수정

### 3. 체크리스트

#### Auth ✅
- [x] LoginE2ETest
- [x] LogoutE2ETest
- [x] GetMeE2ETest

#### Users ✅
- [x] CreateUserE2ETest
- [x] ReadUserE2ETest
- [x] UpdateUserE2ETest
- [x] DeleteUserE2ETest

#### Departments ⚠️
- [x] CreateDepartmentE2ETest
- [x] ReadDepartmentE2ETest
- [ ] UpdateDepartmentE2ETest - **TODO**
- [ ] DeleteDepartmentE2ETest - **TODO**

#### Students ⚠️
- [x] CreateStudentE2ETest
- [ ] ReadStudentE2ETest - **TODO**
- [ ] UpdateStudentE2ETest - **TODO**
- [ ] DeleteStudentE2ETest - **TODO**

#### Classrooms ❌
- [ ] CreateClassroomE2ETest - **TODO**
- [ ] ReadClassroomE2ETest - **TODO**
- [ ] UpdateClassroomE2ETest - **TODO**
- [ ] DeleteClassroomE2ETest - **TODO**

#### Subjects ⚠️
- [x] CreateSubjectE2ETest
- [ ] ReadSubjectE2ETest - **TODO**
- [ ] UpdateSubjectE2ETest - **TODO**
- [ ] DeleteSubjectE2ETest - **TODO**

#### Lessons ⚠️
- [x] ReadLessonE2ETest
- [x] GetMyLessonsE2ETest
- [ ] UpdateAttendanceE2ETest - **TODO**

#### Requests - Absence ⚠️
- [x] CreateAbsenceRequestE2ETest
- [x] ReadAbsenceRequestE2ETest
- [ ] UpdateAbsenceRequestStatusE2ETest - **TODO**

#### Requests - Exchange ❌
- [ ] CreateLessonExchangeRequestE2ETest - **TODO**
- [ ] CreateSubjectExchangeRequestE2ETest - **TODO**
- [ ] ReadExchangeRequestE2ETest - **TODO**
- [ ] UpdateExchangeRequestStatusE2ETest - **TODO**

#### Requests - Purchase ⚠️
- [x] CreatePurchaseRequestE2ETest
- [ ] ReadPurchaseRequestE2ETest - **TODO**
- [ ] UpdatePurchaseRequestStatusE2ETest - **TODO**

## 템플릿 코드

### CRUD 템플릿

#### Create Test
```java
@Tag("e2e")
@DisplayName("{Domain} E2E Tests - Create")
class Create{Domain}E2ETest extends Base{Domain}E2ETest {

    @Test
    @DisplayName("{도메인} 생성 성공")
    void create{Domain}Success() {
        String sessionId = getAdminSession();
        var request = create{Domain}Request(...);

        given()
            .sessionId(sessionId)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
        .when()
            .post("/api/v1/{domains}")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("data.id", notNullValue());
    }

    @Test
    @DisplayName("{도메인} 생성 실패 - 권한 없음")
    void create{Domain}FailUnauthorized() {
        var request = create{Domain}Request(...);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
        .when()
            .post("/api/v1/{domains}")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("{도메인} 생성 실패 - 필수 필드 누락")
    void create{Domain}FailMissingFields() {
        String sessionId = getAdminSession();

        given()
            .sessionId(sessionId)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("{}")
        .when()
            .post("/api/v1/{domains}")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
```

#### Read Test
```java
@Tag("e2e")
@DisplayName("{Domain} E2E Tests - Read")
class Read{Domain}E2ETest extends Base{Domain}E2ETest {

    @Test
    @DisplayName("{도메인} 상세 조회 성공")
    void get{Domain}ByIdSuccess() {
        {Domain} entity = {domain}Repository.save(new {Domain}(...));
        String sessionId = getAdminSession();

        given()
            .sessionId(sessionId)
        .when()
            .get("/api/v1/{domains}/" + entity.getId())
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.id", equalTo(entity.getId().intValue()));
    }

    @Test
    @DisplayName("{도메인} 상세 조회 실패 - 존재하지 않는 ID")
    void get{Domain}ByIdFailNotFound() {
        String sessionId = getAdminSession();

        given()
            .sessionId(sessionId)
        .when()
            .get("/api/v1/{domains}/99999")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("{도메인} 목록 조회 성공")
    void get{Domain}sSuccess() {
        String sessionId = getAdminSession();

        given()
            .sessionId(sessionId)
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/v1/{domains}")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.totalCount", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("{도메인} 조회 실패 - 권한 없음")
    void get{Domain}sFailUnauthorized() {
        given()
        .when()
            .get("/api/v1/{domains}")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
```

#### Update Test
```java
@Tag("e2e")
@DisplayName("{Domain} E2E Tests - Update")
class Update{Domain}E2ETest extends Base{Domain}E2ETest {

    @Test
    @DisplayName("{도메인} 수정 성공")
    void update{Domain}Success() {
        {Domain} entity = {domain}Repository.save(new {Domain}(...));
        String sessionId = getAdminSession();

        Map<String, Object> updateRequest = Map.of(
            "field1", "Updated Value"
        );

        given()
            .sessionId(sessionId)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(updateRequest)
        .when()
            .put("/api/v1/{domains}/" + entity.getId())
        .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("{도메인} 수정 실패 - 존재하지 않는 ID")
    void update{Domain}FailNotFound() {
        String sessionId = getAdminSession();
        Map<String, Object> updateRequest = Map.of("field1", "Value");

        given()
            .sessionId(sessionId)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(updateRequest)
        .when()
            .put("/api/v1/{domains}/99999")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("{도메인} 수정 실패 - 권한 없음")
    void update{Domain}FailUnauthorized() {
        {Domain} entity = {domain}Repository.save(new {Domain}(...));
        Map<String, Object> updateRequest = Map.of("field1", "Value");

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(updateRequest)
        .when()
            .put("/api/v1/{domains}/" + entity.getId())
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
```

#### Delete Test
```java
@Tag("e2e")
@DisplayName("{Domain} E2E Tests - Delete")
class Delete{Domain}E2ETest extends Base{Domain}E2ETest {

    @Test
    @DisplayName("{도메인} 삭제 성공")
    void delete{Domain}Success() {
        {Domain} entity = {domain}Repository.save(new {Domain}(...));
        String sessionId = getAdminSession();

        given()
            .sessionId(sessionId)
        .when()
            .delete("/api/v1/{domains}/" + entity.getId())
        .then()
            .statusCode(HttpStatus.OK.value());

        // Verify deletion
        given()
            .sessionId(sessionId)
        .when()
            .get("/api/v1/{domains}/" + entity.getId())
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("{도메인} 삭제 실패 - 존재하지 않는 ID")
    void delete{Domain}FailNotFound() {
        String sessionId = getAdminSession();

        given()
            .sessionId(sessionId)
        .when()
            .delete("/api/v1/{domains}/99999")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("{도메인} 삭제 실패 - 권한 없음")
    void delete{Domain}FailUnauthorized() {
        {Domain} entity = {domain}Repository.save(new {Domain}(...));

        given()
        .when()
            .delete("/api/v1/{domains}/" + entity.getId())
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
```

## 추정 작업 시간

- **세션/권한 문제 해결**: 2-4시간
- **나머지 CRUD 테스트 완성**: 4-6시간
- **Exchange Request 테스트**: 2-3시간
- **통합 테스트 및 리팩토링**: 2-3시간

**총 예상 시간**: 10-16시간

## 완성 후 확인 사항

```bash
# 모든 테스트 성공 확인
./gradlew test --tests "*E2ETest"

# 커버리지 확인
./gradlew test jacocoTestReport
# 리포트: build/reports/jacoco/test/html/index.html

# 최종 통계
find src/test -name "*E2ETest.java" | wc -l  # 테스트 파일 수
grep -r "@Test" src/test/java/org/geumjeong/learning/sonmoum_api/e2e/ | wc -l  # 테스트 메서드 수
```

## 다음 단계

1. ✅ 인프라 완성 (완료)
2. 🔧 **현재**: 테스트 디버깅
3. 📝 나머지 테스트 작성
4. 🎯 전체 테스트 통과
5. 📊 커버리지 측정
6. 🚀 CI/CD 통합
