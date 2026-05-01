package geumjeongyahak.e2e.auth;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.domain.auth.v1.dto.request.LocalSignupRequest;
import geumjeongyahak.domain.auth.v1.dto.request.RefreshTokenRequest;
import geumjeongyahak.domain.auth.v1.dto.response.TokenResponse;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: 로그아웃 테스트")
class AuthLogoutTest extends AuthBaseTest {

    @Test
    @DisplayName("유효한 Refresh Token으로 로그아웃 성공(200 OK)")
    void logout_Success() {
        // 로그인
        LocalLoginRequest loginReq = new LocalLoginRequest(
                TEST_ADMIN_EMAIL,
                TEST_ADMIN_PASSWORD
        );

        var loginResponse = given()
            .contentType(ContentType.JSON)
            .body(loginReq)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .extract()
            .as(TokenResponse.class);

        // 로그아웃
        String logoutReq = """
            {
                "refreshToken": "%s"
            }
            """.formatted(loginResponse.refreshToken());

        given()
            .contentType(ContentType.JSON)
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(200)
            .body("message", equalTo("로그아웃되었습니다."))
            .log().all();

        // 로그아웃된 토큰으로 재발급 시도 시 실패
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(
                loginResponse.refreshToken()
        );

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
        String logoutReq = """
            {
                "refreshToken": "invalid-token-12345"
            }
            """;

        // 로그아웃은 멱등성을 보장하므로 잘못된 토큰이라도 200 반환
        given()
            .contentType(ContentType.JSON)
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(200)
            .log().all();
    }

    @Test
    @DisplayName("빈 Refresh Token으로 로그아웃 실패(400 Bad Request)")
    void logout_EmptyToken() {
        String logoutReq = """
            {
                "refreshToken": ""
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(400)
            .log().all();
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
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("이미 로그아웃된 토큰으로 재로그아웃 - 멱등성 보장(200 OK)")
    void logout_AlreadyLoggedOut() {
        // 로그인
        LocalLoginRequest loginReq = new LocalLoginRequest(
                TEST_ADMIN_EMAIL,
                TEST_ADMIN_PASSWORD
        );

        var loginResponse = given()
            .contentType(ContentType.JSON)
            .body(loginReq)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .extract()
            .as(TokenResponse.class);

        String logoutReq = """
            {
                "refreshToken": "%s"
            }
            """.formatted(loginResponse.refreshToken());

        // 첫 번째 로그아웃 성공
        given()
            .contentType(ContentType.JSON)
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(200);

        // 두 번째 로그아웃 시도 - 멱등성 보장으로 200 반환
        given()
            .contentType(ContentType.JSON)
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(200)
            .log().all();
    }

    @Test
    @DisplayName("전체 디바이스 로그아웃 성공(200 OK)")
    void logoutAllDevices_Success() {
        String uniqueUsername = "multidevice" + System.currentTimeMillis();

        // 회원가입
        LocalSignupRequest signupReq = new LocalSignupRequest(
                "password123!",
                "multi-device-test",
                "멀티 디바이스 테스트",
                uniqueUsername + "@test.com",
                null,
                "010-1234-5678"
        );

        var signupResponse = given()
            .contentType(ContentType.JSON)
            .body(signupReq)
        .when()
            .post("/signup")
        .then()
            .statusCode(201)
            .extract()
            .as(TokenResponse.class);

        // 추가 로그인 (다른 디바이스 시뮬레이션)
        LocalLoginRequest loginReq = new LocalLoginRequest(
                uniqueUsername + "@test.com",
                "password123!"
        );

        var login2Response = given()
            .contentType(ContentType.JSON)
            .body(loginReq)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .extract()
            .as(TokenResponse.class);

        var login3Response = given()
            .contentType(ContentType.JSON)
            .body(loginReq)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .extract()
            .as(TokenResponse.class);

        // 전체 디바이스 로그아웃
        given()
            .header(AUTH_HEADER, getAuthHeader(signupResponse.accessToken()))
        .when()
            .post("/logout-all")
        .then()
            .statusCode(200)
            .body("message", equalTo("모든 디바이스에서 로그아웃되었습니다."))
            .log().all();

        // 모든 Refresh Token이 무효화되었는지 확인
        RefreshTokenRequest refreshReq1 = new RefreshTokenRequest(signupResponse.refreshToken());
        given()
            .contentType(ContentType.JSON)
            .body(refreshReq1)
        .when()
            .post("/refresh")
        .then()
            .statusCode(401);

        RefreshTokenRequest refreshReq2 = new RefreshTokenRequest(login2Response.refreshToken());
        given()
            .contentType(ContentType.JSON)
            .body(refreshReq2)
        .when()
            .post("/refresh")
        .then()
            .statusCode(401);

        RefreshTokenRequest refreshReq3 = new RefreshTokenRequest(login3Response.refreshToken());
        given()
            .contentType(ContentType.JSON)
            .body(refreshReq3)
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
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("만료된 Access Token으로 전체 디바이스 로그아웃 실패(401 Unauthorized)")
    void logoutAllDevices_ExpiredToken() {
        // 만료된 토큰 시뮬레이션 (짧은 만료 시간으로 생성)
        String expiredToken = userTestHelper.generateAccessTokenByNickname(TEST_ADMIN_USERNAME, -1L);

        given()
            .header(AUTH_HEADER, getAuthHeader(expiredToken))
        .when()
            .post("/logout-all")
        .then()
            .statusCode(401)
            .log().all();
    }
}
