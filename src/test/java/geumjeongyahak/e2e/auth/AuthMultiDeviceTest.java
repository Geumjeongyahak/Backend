package geumjeongyahak.e2e.auth;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.domain.auth.v1.dto.request.LogoutRequest;
import geumjeongyahak.domain.auth.v1.dto.request.RefreshTokenRequest;
import geumjeongyahak.domain.auth.v1.dto.response.TokenResponse;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@DisplayName("E2E: 여러 기기 로그인 세션 유지")
class AuthMultiDeviceTest extends AuthBaseTest {

    private static final String PASSWORD = "password123!";

    private String email;

    @BeforeEach
    void createUser() {
        email = "multi-device-" + System.nanoTime() + "@test.com";
        userTestHelper.createTestUser(email, "여러 기기 사용자", PASSWORD, RoleType.GUEST);
    }

    private TokenResponse login() {
        return given()
            .contentType(ContentType.JSON)
            .body(new LocalLoginRequest(email, PASSWORD))
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .extract()
            .as(TokenResponse.class);
    }

    private TokenResponse refreshOk(String refreshToken) {
        return given()
            .contentType(ContentType.JSON)
            .body(new RefreshTokenRequest(refreshToken))
        .when()
            .post("/refresh")
        .then()
            .statusCode(200)
            .extract()
            .as(TokenResponse.class);
    }

    private void refreshRejected(String refreshToken) {
        given()
            .contentType(ContentType.JSON)
            .body(new RefreshTokenRequest(refreshToken))
        .when()
            .post("/refresh")
        .then()
            .statusCode(401)
            .body("code", equalTo("AUTH004"));
    }

    @Test
    @DisplayName("같은 계정으로 두 번 로그인하면 두 refresh 토큰 모두 재발급된다")
    void twoLogins_bothRefreshTokensWork() {
        TokenResponse pc = login();
        TokenResponse phone = login();

        refreshOk(pc.refreshToken());
        refreshOk(phone.refreshToken());
    }

    @Test
    @DisplayName("재발급에 쓴 refresh 토큰은 다시 쓸 수 없다")
    void usedRefreshToken_cannotBeReused() {
        TokenResponse pc = login();

        refreshOk(pc.refreshToken());

        refreshRejected(pc.refreshToken());
    }

    @Test
    @DisplayName("한 기기에서 재발급해도 다른 기기 토큰은 남는다")
    void refreshOnOneDevice_keepsOtherDevice() {
        TokenResponse pc = login();
        TokenResponse phone = login();

        TokenResponse pc2 = refreshOk(pc.refreshToken());

        refreshOk(phone.refreshToken());
        refreshOk(pc2.refreshToken());
    }

    @Test
    @DisplayName("6번 로그인하면 가장 오래된 토큰만 무효가 된다")
    void sixLogins_onlyOldestInvalidated() {
        List<TokenResponse> logins = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            logins.add(login());
        }

        refreshRejected(logins.get(0).refreshToken());
        for (int i = 1; i < 6; i++) {
            refreshOk(logins.get(i).refreshToken());
        }
    }

    @Test
    @DisplayName("전체 로그아웃하면 모든 토큰이 무효가 된다")
    void logoutAll_invalidatesEveryToken() {
        TokenResponse pc = login();
        TokenResponse phone = login();

        given()
            .header(AUTH_HEADER, getAuthHeader(pc.accessToken()))
        .when()
            .post("/logout-all")
        .then()
            .statusCode(200);

        refreshRejected(pc.refreshToken());
        refreshRejected(phone.refreshToken());
    }

    @Test
    @DisplayName("한 기기 로그아웃은 그 토큰만 무효로 한다")
    void singleLogout_invalidatesOnlyThatToken() {
        TokenResponse pc = login();
        TokenResponse phone = login();

        given()
            .contentType(ContentType.JSON)
            .body(new LogoutRequest(pc.refreshToken()))
        .when()
            .post("/logout")
        .then()
            .statusCode(200);

        refreshRejected(pc.refreshToken());
        refreshOk(phone.refreshToken());
    }
}
