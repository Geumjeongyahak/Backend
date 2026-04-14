package geumjeongyahak.e2e.users;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.v1.dto.request.CreateUserRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserResponse;

import static io.restassured.RestAssured.given;

@DisplayName("E2E: User 삭제 테스트")
class UserDeleteTest extends UserBaseTest {

    @Test
    @DisplayName("관리자 권한으로 User 삭제 성공(204 No Content)")
    void deleteUser_Success() {
        // 먼저 사용자 생성
        CreateUserRequest createReq = new CreateUserRequest(
                "Delete Test User",
                "deletetest@test.com",
                "pw_deletetest",
                "deletetest@test.com",
                "010-3333-4444",
                RoleType.ROLE_GUEST.name()
        );

        var createdUser = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(createReq)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(createdUser.username());

        // 삭제 요청
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}", createdUser.id())
        .then()
            .statusCode(204)
            .log().all();

        // 삭제 확인 - 조회 시 404 반환
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", createdUser.id())
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 User 삭제 실패(403 Forbidden)")
    void deleteUser_Forbidden() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .delete("/{userId}", 1L)
        .then()
            .statusCode(403)
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 User 삭제 실패(404 Not Found)")
    void deleteUser_NotFound() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}", 99999L)
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 User 삭제 실패(401 Unauthorized)")
    void deleteUser_Unauthorized() {
        given()
        .when()
            .delete("/{userId}", 1L)
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("같은 User 두 번 삭제 시 두 번째는 실패(404 Not Found)")
    void deleteUser_AlreadyDeleted() {
        // 먼저 사용자 생성
        CreateUserRequest createReq = new CreateUserRequest(
                "Double Delete Test",
                "doubledelete@test.com",
                "pw_doubledelete",
                "doubledelete@test.com",
                "010-5555-6666",
                RoleType.ROLE_GUEST.name()
        );

        var createdUser = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(createReq)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .as(UserResponse.class);

        userTestHelper.setUser(createdUser.username());

        // 첫 번째 삭제
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}", createdUser.id())
        .then()
            .statusCode(204);

        // 두 번째 삭제 시도
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}", createdUser.id())
        .then()
            .statusCode(404)
            .log().all();
    }
}
