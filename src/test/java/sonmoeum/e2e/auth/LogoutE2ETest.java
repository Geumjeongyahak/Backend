package sonmoeum.e2e.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;

@Tag("e2e")
@DisplayName("Auth E2E Tests - Logout")
class LogoutE2ETest extends BaseAuthE2ETest {

    @Test
    @DisplayName("로그아웃 성공")
    void logoutSuccess() {
        String sessionCookie = getAuthenticatedSession();

        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .post("/api/v1/auth/logout")
        .then()
            .statusCode(HttpStatus.OK.value());

        // Verify session is invalid after logout
        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .get("/api/v1/auth/me")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("로그아웃 실패 - 인증되지 않은 요청")
    void logoutFailUnauthenticated() {
        given()
        .when()
            .post("/api/v1/auth/logout")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
