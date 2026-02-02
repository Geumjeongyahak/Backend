package sonmoeum.e2e.auth;

import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;

@Tag("e2e")
@DisplayName("Auth E2E Tests - Get Me")
class GetMeE2ETest extends BaseAuthE2ETest {

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMeSuccess() {
        String email = "test@example.com";
        String password = "password";
        createTestUser(email, password);

        String sessionCookie = performLogin(email, password);

        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .get("/api/v1/auth/me")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.email", equalTo(email))
            .body("data.name", equalTo("Test User"));
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 인증되지 않은 요청")
    void getMeFailUnauthenticated() {
        given()
        .when()
            .get("/api/v1/auth/me")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("내 정보 조회 - 관리자")
    void getMeAsAdmin() {
        String email = "admin@example.com";
        String password = "admin123";
        createAdminUser(email, password);

        String sessionCookie = performLogin(email, password);

        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .get("/api/v1/auth/me")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.email", equalTo(email))
            .body("data.name", equalTo("Admin User"))
            .body("data.role", equalTo("MANAGER")); // RoleType.MANAGER
    }
}
