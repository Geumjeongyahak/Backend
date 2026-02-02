package sonmoeum.e2e.users;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import sonmoeum.domain.users.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Tag("e2e")
@DisplayName("User E2E Tests - Update")
class UpdateUserE2ETest extends BaseUserE2ETest {

    @Test
    @DisplayName("사용자 수정 성공")
    void updateUserSuccess() {
        User user = createTestUser("test@example.com", "password");
        String sessionCookie = getAdminSession();

        Map<String, Object> updateRequest = Map.of(
            "name", "Updated Name",
            "phoneNumber", "010-9999-9999"
        );

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(updateRequest)
        .when()
            .put("/api/v1/users/" + user.getId())
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.name", equalTo("Updated Name"))
            .body("data.phoneNumber", equalTo("010-9999-9999"));
    }

    @Test
    @DisplayName("사용자 수정 실패 - 존재하지 않는 ID")
    void updateUserFailNotFound() {
        String sessionCookie = getAdminSession();

        Map<String, Object> updateRequest = Map.of(
            "name", "Updated Name"
        );

        given()
            .cookie("SESSION", sessionCookie)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(updateRequest)
        .when()
            .put("/api/v1/users/99999")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("사용자 수정 실패 - 권한 없음")
    void updateUserFailUnauthorized() {
        User user = createTestUser("test@example.com", "password");

        Map<String, Object> updateRequest = Map.of(
            "name", "Updated Name"
        );

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(updateRequest)
        .when()
            .put("/api/v1/users/" + user.getId())
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
