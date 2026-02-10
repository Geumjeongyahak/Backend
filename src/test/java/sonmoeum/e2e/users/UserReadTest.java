package sonmoeum.e2e.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: User 조회 테스트")
class UserReadTest extends UserBaseTest {

    @Test
    @DisplayName("관리자 권한으로 특정 User 조회 성공(200 OK)")
    void getUserById_Success() {
        Long userId = 1L; // admin user from init_data.sql

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", userId)
        .then()
            .statusCode(200)
            .body("id", equalTo(userId.intValue()))
            .body("username", equalTo(TEST_ADMIN_USERNAME))
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 User 조회 실패(404 Not Found)")
    void getUserById_NotFound() {
        Long nonExistentId = 99999L;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", nonExistentId)
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 User 조회 실패(403 Forbidden)")
    void getUserById_Forbidden() {
        Long userId = 1L;

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/{userId}", userId)
        .then()
            .statusCode(403)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 User 조회 실패(401 Unauthorized)")
    void getUserById_Unauthorized() {
        Long userId = 1L;

        given()
        .when()
            .get("/{userId}", userId)
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("본인 정보 조회 성공(200 OK) - admin")
    void getCurrentUser_Success_Admin() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/me")
        .then()
            .statusCode(200)
            .body("username", equalTo(TEST_ADMIN_USERNAME))
            .body("roles", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("본인 정보 조회 성공(200 OK) - volunteer")
    void getCurrentUser_Success_Volunteer() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/me")
        .then()
            .statusCode(200)
            .body("username", equalTo(TEST_VOLUNTEER_USERNAME))
            .body("roles", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 본인 정보 조회 실패(401 Unauthorized)")
    void getCurrentUser_Unauthorized() {
        given()
        .when()
            .get("/me")
        .then()
            .statusCode(401)
            .log().all();
    }
}
