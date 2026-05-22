package geumjeongyahak.e2e.auth;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.domain.auth.v1.dto.request.LocalSignupRequest;
import geumjeongyahak.domain.auth.v1.dto.request.LogoutRequest;
import geumjeongyahak.domain.auth.v1.dto.response.TokenResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("E2E: 로그아웃 테스트")
class AuthLogoutTest extends AuthBaseTest {

    @Test
    @DisplayName("로그아웃 성공(200 OK)")
    void logout_Success() {
        // 먼저 로그인
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

        // 로그아웃 요청 (RefreshToken 필요)
        LogoutRequest logoutReq = new LogoutRequest(loginResponse.refreshToken());
        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(loginResponse.accessToken()))
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(200);

        // NOTE: 현재 시스템은 stateless JWT를 사용하며 블랙리스트가 구현되어 있지 않음.
        // 또한 /logout 엔드포인트는 permitAll() 설정되어 있어 토큰 검증 여부와 관계없이 접근 가능함.
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 로그아웃 시도 시에도 성공(200 OK) - permitAll 엔드포인트")
    void logout_InvalidToken_Success() {
        LogoutRequest logoutReq = new LogoutRequest("invalid-refresh-token");
        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, "Bearer invalid-token")
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(200); // permitAll이므로 필터에서 401을 내지 않음
    }

    @Test
    @DisplayName("토큰 없이 로그아웃 시도 시에도 성공(200 OK) - permitAll 엔드포인트")
    void logout_NoToken_Success() {
        LogoutRequest logoutReq = new LogoutRequest("some-refresh-token");
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

        // 회원가입
        LocalSignupRequest signupReq = new LocalSignupRequest(
                "password123!",
                "멀티 디바이스 테스트",
                uniqueUsername + "@test.com",
                null,
                "010-1234-5678",
                "900101"
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

        // 로그인 (두 번의 세션을 시뮬레이션하기 위해)
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

        // 전체 로그아웃 요청 (인증 필요)
        given()
            .header(AUTH_HEADER, getAuthHeader(loginResponse1.accessToken()))
        .when()
            .post("/logout-all")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("만료된 토큰으로 로그아웃 시도 성공(200 OK)")
    void logout_ExpiredToken_Success() {
        String expiredToken = userTestHelper.generateAccessTokenByEmail(TEST_ADMIN_EMAIL, -1L);
        LogoutRequest logoutReq = new LogoutRequest("some-refresh-token");

        given()
            .contentType(ContentType.JSON)
            .header(AUTH_HEADER, getAuthHeader(expiredToken))
            .body(logoutReq)
        .when()
            .post("/logout")
        .then()
            .statusCode(200);
    }
}
