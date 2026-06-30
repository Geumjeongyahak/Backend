package geumjeongyahak.e2e.auth;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.domain.auth.v1.dto.request.LogoutRequest;
import geumjeongyahak.domain.auth.v1.dto.request.RefreshTokenRequest;
import geumjeongyahak.domain.auth.v1.dto.response.TokenResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@DisplayName("E2E: 로그아웃 테스트")
class AuthLogoutTest extends AuthBaseTest {

    @Test
    @DisplayName("유효한 Refresh Token으로 로그아웃 성공(200 OK)")
    void logout_Success() {
        LocalLoginRequest loginReq = new LocalLoginRequest(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);

        var loginResponse = given()
            .contentType(ContentType.JSON)
            .body(loginReq)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .extract()
            .as(TokenResponse.class);

        LogoutRequest logoutReq = new LogoutRequest(loginResponse.refreshToken());
        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(loginResponse.accessToken()))
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(200)
            .body("message", equalTo("로그아웃되었습니다."));

        RefreshTokenRequest refreshReq = new RefreshTokenRequest(loginResponse.refreshToken());
        given()
            .contentType(ContentType.JSON)
            .body(refreshReq)
        .when()
            .post("/refresh")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("잘못된 Refresh Token으로 로그아웃 - 멱등성 보장(200 OK)")
    void logout_InvalidToken() {
        LogoutRequest logoutReq = new LogoutRequest("invalid-refresh-token");
        given()
            .contentType(ContentType.JSON)
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("빈 Refresh Token으로 로그아웃 실패(400 Bad Request)")
    void logout_EmptyToken() {
        LogoutRequest logoutReq = new LogoutRequest("");
        given()
            .contentType(ContentType.JSON)
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Refresh Token 누락 시 로그아웃 실패(400 Bad Request)")
    void logout_MissingToken() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/logout")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("이미 로그아웃된 토큰으로 재로그아웃 - 멱등성 보장(200 OK)")
    void logout_AlreadyLoggedOut() {
        LocalLoginRequest loginReq = new LocalLoginRequest(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);

        var loginResponse = given()
            .contentType(ContentType.JSON)
            .body(loginReq)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .extract()
            .as(TokenResponse.class);

        LogoutRequest logoutReq = new LogoutRequest(loginResponse.refreshToken());
        given()
            .contentType(ContentType.JSON)
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(200);

        given()
            .contentType(ContentType.JSON)
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("전체 디바이스 로그아웃 성공(200 OK)")
    void logoutAllDevices_Success() {
        String uniqueUsername = "multidevice" + System.currentTimeMillis();
        userTestHelper.createTestUser(
            uniqueUsername + "@test.com",
            "멀티 디바이스 테스트",
            "password123!",
            RoleType.GUEST
        );

        LocalLoginRequest loginReq = new LocalLoginRequest(uniqueUsername + "@test.com", "password123!");

        var loginResponse1 = given()
            .contentType(ContentType.JSON)
            .body(loginReq)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .extract()
            .as(TokenResponse.class);

        var loginResponse2 = given()
            .contentType(ContentType.JSON)
            .body(loginReq)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .extract()
            .as(TokenResponse.class);

        given()
            .header(AUTH_HEADER, getAuthHeader(loginResponse1.accessToken()))
        .when()
            .post("/logout-all")
        .then()
            .statusCode(200)
            .body("message", equalTo("모든 디바이스에서 로그아웃되었습니다."));

        given()
            .contentType(ContentType.JSON)
            .body(new RefreshTokenRequest(loginResponse1.refreshToken()))
        .when()
            .post("/refresh")
        .then()
            .statusCode(401);

        given()
            .contentType(ContentType.JSON)
            .body(new RefreshTokenRequest(loginResponse2.refreshToken()))
        .when()
            .post("/refresh")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("인증 없이 전체 디바이스 로그아웃 실패(401 Unauthorized)")
    void logoutAllDevices_Unauthorized() {
        given()
        .when()
            .post("/logout-all")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("만료된 Access Token으로 전체 디바이스 로그아웃 실패(401 Unauthorized)")
    void logoutAllDevices_ExpiredToken() {
        String expiredToken = userTestHelper.generateAccessTokenByEmail(TEST_ADMIN_EMAIL, -1L);

        given()
            .header(AUTH_HEADER, getAuthHeader(expiredToken))
        .when()
            .post("/logout-all")
        .then()
            .statusCode(401);
    }
}
