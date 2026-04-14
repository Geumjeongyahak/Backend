package geumjeongyahak.e2e.auth;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.domain.auth.v1.dto.request.RefreshTokenRequest;
import geumjeongyahak.domain.auth.v1.dto.response.TokenResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: 토큰 재발급 테스트")
class AuthRefreshTest extends AuthBaseTest {

    @Test
    @DisplayName("유효한 Refresh Token으로 토큰 재발급 성공(200 OK)")
    void refreshToken_Success() {
        // 먼저 로그인하여 토큰 획득
        LocalLoginRequest loginReq = new LocalLoginRequest(
                TEST_ADMIN_USERNAME,
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

        // Refresh Token으로 새 토큰 발급
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(
                loginResponse.refreshToken()
        );

        var newTokens = given()
            .contentType(ContentType.JSON)
            .body(refreshReq)
        .when()
            .post("/refresh")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue())
            .body("tokenType", equalTo("Bearer"))
            .body("accessTokenExpiresAt", notNullValue())
            .body("refreshTokenExpiresAt", notNullValue())
            .log().all()
            .extract()
            .as(TokenResponse.class);

        // 새로 발급받은 Access Token이 작동하는지 확인 (basePath 임시 초기화)
        String originalBasePath = RestAssured.basePath;
        RestAssured.basePath = "";

        given()
            .header(AUTH_HEADER, getAuthHeader(newTokens.accessToken()))
        .when()
            .get("/api/v1/users/me")
        .then()
            .statusCode(200);

        RestAssured.basePath = originalBasePath;
    }

    @Test
    @DisplayName("잘못된 Refresh Token으로 재발급 실패(401 Unauthorized)")
    void refreshToken_InvalidToken() {
        RefreshTokenRequest req = new RefreshTokenRequest(
                "invalid-refresh-token-12345"
        );

        given()
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/refresh")
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("빈 Refresh Token으로 재발급 실패(400 Bad Request)")
    void refreshToken_EmptyToken() {
        String invalidReq = """
            {
                "refreshToken": ""
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(invalidReq)
        .when()
            .post("/refresh")
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("Refresh Token 누락 시 재발급 실패(400 Bad Request)")
    void refreshToken_MissingToken() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/refresh")
        .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("로그아웃된 Refresh Token으로 재발급 실패(401 Unauthorized)")
    void refreshToken_LoggedOutToken() {
        // 로그인
        LocalLoginRequest loginReq = new LocalLoginRequest(
                TEST_ADMIN_USERNAME,
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
            .statusCode(200);

        // 로그아웃된 토큰으로 재발급 시도
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(
                loginResponse.refreshToken()
        );

        given()
            .contentType(ContentType.JSON)
            .body(refreshReq)
        .when()
            .post("/refresh")
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("동일한 Refresh Token으로 여러 번 재발급 가능")
    void refreshToken_MultipleTimes() {
        // 로그인
        LocalLoginRequest loginReq = new LocalLoginRequest(
                TEST_ADMIN_USERNAME,
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

        RefreshTokenRequest refreshReq = new RefreshTokenRequest(
                loginResponse.refreshToken()
        );

        // 첫 번째 재발급
        var firstRefresh = given()
            .contentType(ContentType.JSON)
            .body(refreshReq)
        .when()
            .post("/refresh")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .extract()
            .as(TokenResponse.class);

        // 첫 번째 재발급 후 새로운 리프레시 토큰 사용
        RefreshTokenRequest newRefreshReq = new RefreshTokenRequest(
                firstRefresh.refreshToken()
        );

        // 두 번째 재발급
        var secondRefresh = given()
            .contentType(ContentType.JSON)
            .body(newRefreshReq)
        .when()
            .post("/refresh")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .extract()
            .as(TokenResponse.class);

        // 세 번째 재발급
        RefreshTokenRequest thirdRefreshReq = new RefreshTokenRequest(
                secondRefresh.refreshToken()
        );

        given()
            .contentType(ContentType.JSON)
            .body(thirdRefreshReq)
        .when()
            .post("/refresh")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .log().all();
    }
}
