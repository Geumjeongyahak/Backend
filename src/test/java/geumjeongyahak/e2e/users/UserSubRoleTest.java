package geumjeongyahak.e2e.users;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.users.v1.dto.request.CreateUserRequest;
import geumjeongyahak.domain.users.v1.dto.request.UserPermissionRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserDetailResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: User Permission 관리 테스트")
class UserSubRoleTest extends UserBaseTest {

    private Long createVolunteer() {
        String unique = "permtest" + System.currentTimeMillis();
        CreateUserRequest req = new CreateUserRequest(
            unique + "@test.com",
            unique,
            "Permission Test User",
            "password123!",
            "010-1234-5678",
            "VOLUNTEER",
            null
        );

        var user = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .as(UserDetailResponse.class);

        userTestHelper.setUser(user.nickname());
        return user.id();
    }

    @Test
    @DisplayName("사용자 권한 목록 조회 성공(200 OK)")
    void getPermissions_Success() {
        Long userId = createVolunteer();

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}/permissions", userId)
        .then()
            .statusCode(200)
            .body("$", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 권한 목록 조회 실패(403 Forbidden)")
    void getPermissions_Forbidden() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/{userId}/permissions", 1L)
        .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"))
            .log().all();
    }

    @Test
    @DisplayName("권한 추가 성공(200 OK)")
    void addPermission_Success() {
        Long userId = createVolunteer();
        UserPermissionRequest req = new UserPermissionRequest("lesson:write:*");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/permissions", userId)
        .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].code", equalTo("lesson:write:*"))
            .log().all();
    }

    @Test
    @DisplayName("여러 권한 추가 성공(200 OK)")
    void addMultiplePermissions_Success() {
        Long userId = createVolunteer();

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new UserPermissionRequest("lesson:write:*"))
        .when()
            .post("/{userId}/permissions", userId)
        .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new UserPermissionRequest("department:read:*"))
        .when()
            .post("/{userId}/permissions", userId)
        .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .log().all();
    }

    @Test
    @DisplayName("중복 권한 추가 시 멱등성 보장(200 OK, 중복 없음)")
    void addPermission_Idempotent() {
        Long userId = createVolunteer();
        UserPermissionRequest req = new UserPermissionRequest("lesson:write:*");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/permissions", userId)
        .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/permissions", userId)
        .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .log().all();
    }

    @Test
    @DisplayName("유효하지 않은 권한 코드로 권한 추가 실패(400 Bad Request)")
    void addPermission_InvalidCode() {
        Long userId = createVolunteer();
        String invalidJson = """
            {"permissionCode": "INVALID_FORMAT"}
            """;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(invalidJson)
        .when()
            .post("/{userId}/permissions", userId)
        .then()
            .statusCode(400)
            .body("code", equalTo("VAL001"))
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 사용자에게 권한 추가 실패(404 Not Found)")
    void addPermission_UserNotFound() {
        UserPermissionRequest req = new UserPermissionRequest("lesson:write:*");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/permissions", 99999L)
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 권한 추가 실패(403 Forbidden)")
    void addPermission_Forbidden() {
        UserPermissionRequest req = new UserPermissionRequest("lesson:write:*");

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/permissions", 1L)
        .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"))
            .log().all();
    }

    @Test
    @DisplayName("권한 제거 성공(200 OK)")
    void removePermission_Success() {
        Long userId = createVolunteer();
        UserPermissionRequest req = new UserPermissionRequest("lesson:write:*");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/{userId}/permissions", userId)
        .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .delete("/{userId}/permissions", userId)
        .then()
            .statusCode(200)
            .body("size()", equalTo(0))
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 권한 제거 시 멱등성 보장(200 OK)")
    void removePermission_Idempotent() {
        Long userId = createVolunteer();
        UserPermissionRequest req = new UserPermissionRequest("lesson:write:*");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .delete("/{userId}/permissions", userId)
        .then()
            .statusCode(200)
            .body("size()", equalTo(0))
            .log().all();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 권한 제거 실패(403 Forbidden)")
    void removePermission_Forbidden() {
        UserPermissionRequest req = new UserPermissionRequest("lesson:write:*");

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .delete("/{userId}/permissions", 1L)
        .then()
            .statusCode(403)
            .body("code", equalTo("AUTHZ001"))
            .log().all();
    }
}
