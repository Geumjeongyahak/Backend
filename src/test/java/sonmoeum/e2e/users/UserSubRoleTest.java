package sonmoeum.e2e.users;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.users.v1.dto.request.AddSubRoleRequest;
import sonmoeum.domain.users.v1.dto.request.CreateUserRequest;
import sonmoeum.domain.users.v1.dto.request.RemoveSubRoleRequest;
import sonmoeum.domain.users.v1.dto.response.UserResponse;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: User SubRole 관리 테스트")
class UserSubRoleTest extends UserBaseTest {

    @Test
    @DisplayName("사용자 역할 목록 조회 성공(200 OK)")
    void getUserRoles_Success() {
        // admin 사용자의 역할 조회 (초기 상태: ROLE_ADMIN만 있음)
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}/roles", 1L)  // admin user ID
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("[0].name", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 역할 조회 실패(403 Forbidden)")
    void getUserRoles_Forbidden() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/{userId}/roles", 1L)
        .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"))  // ACCESS_DENIED
            .log().all();
    }

    @Test
    @DisplayName("하위권한 추가 성공 - TEACHER 역할(200 OK)")
    void addSubRole_Success_Teacher() {
        // 테스트용 사용자 생성
        String uniqueUsername = "subrole" + System.currentTimeMillis();
        CreateUserRequest createReq = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "SubRole Test User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                RoleType.ROLE_VOLUNTEER.name()
        );

        var user = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(createReq)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(user.username());

        // TEACHER 하위권한 추가
        AddSubRoleRequest addReq = new AddSubRoleRequest("TEACHER");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(addReq)
        .when()
            .post("/{userId}/roles", user.id())
        .then()
            .statusCode(200)
            .body("size()", equalTo(2))  // ROLE_VOLUNTEER + TEACHER
            .body("find { it.name == 'TEACHER' }", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("하위권한 추가 성공 - DEPT_GENERAL_AFFAIRS 역할(200 OK)")
    void addSubRole_Success_DeptGeneralAffairs() {
        // 테스트용 사용자 생성
        String uniqueUsername = "deptfinance" + System.currentTimeMillis();
        CreateUserRequest createReq = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Dept Finance Test User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                RoleType.ROLE_VOLUNTEER.name()
        );

        var user = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(createReq)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(user.username());

        // DEPT_GENERAL_AFFAIRS 하위권한 추가
        AddSubRoleRequest addReq = new AddSubRoleRequest("DEPT_GENERAL_AFFAIRS");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(addReq)
        .when()
            .post("/{userId}/roles", user.id())
        .then()
            .statusCode(200)
            .body("size()", equalTo(2))  // ROLE_VOLUNTEER + DEPT_GENERAL_AFFAIRS
            .body("find { it.name == 'DEPT_GENERAL_AFFAIRS' }", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("여러 하위권한 추가 성공(200 OK)")
    void addSubRole_Success_Multiple() {
        // 테스트용 사용자 생성
        String uniqueUsername = "multirole" + System.currentTimeMillis();
        CreateUserRequest createReq = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Multi Role Test User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                RoleType.ROLE_VOLUNTEER.name()
        );

        var user = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(createReq)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(user.username());

        // TEACHER 추가
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new AddSubRoleRequest("TEACHER"))
        .when()
            .post("/{userId}/roles", user.id())
        .then()
            .statusCode(200);

        // DEPT_EDUCATION_RESEARCH 추가
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new AddSubRoleRequest("DEPT_EDUCATION_RESEARCH"))
        .when()
            .post("/{userId}/roles", user.id())
        .then()
            .statusCode(200)
            .body("size()", equalTo(3))  // ROLE_VOLUNTEER + TEACHER + DEPT_EDUCATION_RESEARCH
            .log().all();
    }

    @Test
    @DisplayName("기본 역할 추가 시도 실패(400 Bad Request)")
    void addSubRole_CannotAddBaseRole() {
        String uniqueUsername = "baserole" + System.currentTimeMillis();
        CreateUserRequest createReq = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Base Role Test User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                RoleType.ROLE_VOLUNTEER.name()
        );

        var user = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(createReq)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(user.username());

        // ROLE_ADMIN (기본 역할) 추가 시도
        AddSubRoleRequest addReq = new AddSubRoleRequest("ROLE_ADMIN");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(addReq)
        .when()
            .post("/{userId}/roles", user.id())
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL001"))  // CANNOT_ASSIGN_BASE_ROLE
            .log().all();
    }

    @Test
    @DisplayName("이미 부여된 역할 중복 추가 실패(409 Conflict)")
    void addSubRole_RoleAlreadyAssigned() {
        String uniqueUsername = "duplicate" + System.currentTimeMillis();
        CreateUserRequest createReq = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Duplicate Role Test User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                RoleType.ROLE_VOLUNTEER.name()
        );

        var user = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(createReq)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(user.username());

        // TEACHER 추가
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new AddSubRoleRequest("TEACHER"))
        .when()
            .post("/{userId}/roles", user.id())
        .then()
            .statusCode(200);

        // TEACHER 중복 추가 시도
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new AddSubRoleRequest("TEACHER"))
        .when()
            .post("/{userId}/roles", user.id())
        .then()
            .statusCode(409)
            .body("code", equalTo("ROLE001"))  // ROLE_ALREADY_ASSIGNED
            .log().all();
    }

    @Test
    @DisplayName("하위권한 제거 성공(200 OK)")
    void removeSubRole_Success() {
        // 테스트용 사용자 생성 및 하위권한 추가
        String uniqueUsername = "removerole" + System.currentTimeMillis();
        CreateUserRequest createReq = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Remove Role Test User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                RoleType.ROLE_VOLUNTEER.name()
        );

        var user = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(createReq)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(user.username());

        // TEACHER 추가
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new AddSubRoleRequest("TEACHER"))
        .when()
            .post("/{userId}/roles", user.id())
        .then()
            .statusCode(200);

        // TEACHER 제거
        RemoveSubRoleRequest removeReq = new RemoveSubRoleRequest("TEACHER");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(removeReq)
        .when()
            .delete("/{userId}/roles", user.id())
        .then()
            .statusCode(200)
//            .body("size()", equalTo(1))  // ROLE_VOLUNTEER만 남음
//            .body("find { it.name == 'TEACHER' }", nullValue())
            .log().all();
    }

    @Test
    @DisplayName("기본 역할 제거 시도 실패(400 Bad Request)")
    void removeSubRole_CannotRemoveBaseRole() {
        // ROLE_VOLUNTEER (기본 역할) 제거 시도
        String uniqueUsername = "removebase" + System.currentTimeMillis();
        CreateUserRequest createReq = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Remove Base Test User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                RoleType.ROLE_VOLUNTEER.name()
        );

        var user = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(createReq)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(user.username());

        RemoveSubRoleRequest removeReq = new RemoveSubRoleRequest("ROLE_VOLUNTEER");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(removeReq)
        .when()
            .delete("/{userId}/roles", user.id())
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL001"))  // 유효성 검사 오류
            .log().all();
    }

    @Test
    @DisplayName("부여되지 않은 역할 제거 시도 실패(400 Bad Request)")
    void removeSubRole_RoleNotAssigned() {
        String uniqueUsername = "notassigned" + System.currentTimeMillis();
        CreateUserRequest createReq = new CreateUserRequest(
                uniqueUsername + "@test.com",
                "password123!",
                "Not Assigned Test User",
                uniqueUsername + "@test.com",
                "010-1234-5678",
                RoleType.ROLE_VOLUNTEER.name()
        );

        var user = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(createReq)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(user.username());

        // 부여되지 않은 TEACHER 제거 시도
        RemoveSubRoleRequest removeReq = new RemoveSubRoleRequest("TEACHER");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(removeReq)
        .when()
            .delete("/{userId}/roles", user.id())
        .then()
            .statusCode(400)
            .body("code", equalTo("ROLE002"))  // ROLE_NOT_ASSIGNED
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 사용자에게 역할 추가 실패(404 Not Found)")
    void addSubRole_UserNotFound() {
        AddSubRoleRequest addReq = new AddSubRoleRequest("TEACHER");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(addReq)
        .when()
            .post("/{userId}/roles", 99999L)  // 존재하지 않는 ID
        .then()
            .statusCode(404)
            .body("code", equalTo("RES002"))  // USER_NOT_FOUND
            .log().all();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 역할 추가 실패(403 Forbidden)")
    void addSubRole_Forbidden() {
        AddSubRoleRequest addReq = new AddSubRoleRequest("TEACHER");

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(addReq)
        .when()
            .post("/{userId}/roles", 1L)
        .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"))  // ACCESS_DENIED
            .log().all();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 역할 제거 실패(403 Forbidden)")
    void removeSubRole_Forbidden() {
        RemoveSubRoleRequest removeReq = new RemoveSubRoleRequest("TEACHER");

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(removeReq)
        .when()
            .delete("/{userId}/roles", 1L)
        .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"))  // ACCESS_DENIED
            .log().all();
    }
}
