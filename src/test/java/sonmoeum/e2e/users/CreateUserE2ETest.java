package sonmoeum.e2e.users;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Tag("e2e")
@DisplayName("User E2E Tests - Create")
class CreateUserE2ETest extends BaseUserE2ETest {

    @Test
    @DisplayName("사용자 생성 성공")
    void createUserSuccess() {
        String sessionCookie = getAdminSession();
        var userRequest = createUserRequest("newuser@example.com", "password123", "New User");

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(userRequest)
        .when()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("data.email", equalTo("newuser@example.com"))
            .body("data.name", equalTo("New User"))
            .body("data.id", notNullValue());
    }

    @Test
    @DisplayName("사용자 생성 실패 - 이메일 중복")
    void createUserFailDuplicateEmail() {
        String email = "duplicate@example.com";
        createTestUser(email, "password");

        String sessionCookie = getAdminSession();
        var userRequest = createUserRequest(email, "password123", "Duplicate User");

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(userRequest)
        .when()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("사용자 생성 실패 - 잘못된 이메일 형식")
    void createUserFailInvalidEmail() {
        String sessionCookie = getAdminSession();
        var userRequest = createUserRequest("invalid-email", "password123", "Invalid User");

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(userRequest)
        .when()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("사용자 생성 실패 - 권한 없음")
    void createUserFailUnauthorized() {
        var userRequest = createUserRequest("newuser@example.com", "password123", "New User");

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(userRequest)
        .when()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("사용자 생성 실패 - 필수 필드 누락")
    void createUserFailMissingFields() {
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("{}")
        .when()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
