package sonmoeum.e2e.users;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import sonmoeum.domain.users.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@Tag("e2e")
@DisplayName("User E2E Tests - Read")
class ReadUserE2ETest extends BaseUserE2ETest {

    @Test
    @DisplayName("사용자 상세 조회 성공")
    void getUserByIdSuccess() {
        User user = createTestUser("test@example.com", "password");
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .get("/api/v1/users/" + user.getId())
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.id", equalTo(user.getId().intValue()))
            .body("data.email", equalTo("test@example.com"))
            .body("data.name", equalTo("Test User"));
    }

    @Test
    @DisplayName("사용자 상세 조회 실패 - 존재하지 않는 ID")
    void getUserByIdFailNotFound() {
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
        .when()
            .get("/api/v1/users/99999")
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("사용자 목록 조회 성공")
    void getUsersSuccess() {
        createTestUser("user1@example.com", "password");
        createTestUser("user2@example.com", "password");
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/v1/users")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content.size()", greaterThan(0))
            .body("data.totalCount", greaterThan(0));
    }

    @Test
    @DisplayName("사용자 목록 조회 - 페이징")
    void getUsersWithPaging() {
        for (int i = 0; i < 5; i++) {
            createTestUser("user" + i + "@example.com", "password");
        }
        String sessionCookie = getAdminSession();

        given()
            .cookie("SESSION", sessionCookie)
            .queryParam("page", 0)
            .queryParam("size", 2)
        .when()
            .get("/api/v1/users")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content.size()", equalTo(2))
            .body("data.currentPage", equalTo(0))
            .body("data.pageSize", equalTo(2));
    }

    @Test
    @DisplayName("사용자 조회 실패 - 권한 없음")
    void getUsersFailUnauthorized() {
        given()
        .when()
            .get("/api/v1/users")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
