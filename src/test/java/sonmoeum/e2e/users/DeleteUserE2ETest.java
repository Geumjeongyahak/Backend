package sonmoeum.e2e.users;

import static io.restassured.RestAssured.given;

import sonmoeum.domain.users.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@Tag("e2e")
@DisplayName("User E2E Tests - Delete")
class DeleteUserE2ETest extends BaseUserE2ETest {

    @Test
    @DisplayName("사용자 삭제 성공")
    void deleteUserSuccess() {
        User user = createTestUser("test@example.com", "password");
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .delete("/api/v1/users/" + user.getId())
        .then()
            .statusCode(HttpStatus.OK.value());

        // Verify deletion
        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .get("/api/v1/users/" + user.getId())
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("사용자 삭제 실패 - 존재하지 않는 ID")
    void deleteUserFailNotFound() {
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .delete("/api/v1/users/99999")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("사용자 삭제 실패 - 권한 없음")
    void deleteUserFailUnauthorized() {
        User user = createTestUser("test@example.com", "password");

        given()
        .when()
            .delete("/api/v1/users/" + user.getId())
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
