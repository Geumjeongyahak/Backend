package sonmoeum.e2e.auth;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;

@Tag("e2e")
@DisplayName("Auth E2E Tests - Login")
class LoginE2ETest extends BaseAuthE2ETest {

    @Test
    @DisplayName("로그인 성공")
    void loginSuccess() {
        String email = "test@example.com";
        String password = "password";
        createTestUser(email, password);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of("email", email, "password", password))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.OK.value())
            .cookie("SESSION") // 쿠키가 설정되었는지 확인
            .body("data.email", equalTo(email))
            .body("data.name", equalTo("Test User"));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void loginFailWrongPassword() {
        String email = "test@example.com";
        String password = "password";
        createTestUser(email, password);

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of("email", email, "password", "wrongpassword"))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    void loginFailUserNotFound() {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of("email", "nonexistent@example.com", "password", "password"))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 이메일 형식")
    void loginFailInvalidEmail() {
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(Map.of("email", "invalid-email", "password", "password"))
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
