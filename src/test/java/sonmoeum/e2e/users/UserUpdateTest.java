package sonmoeum.e2e.users;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.users.v1.dto.request.CreateUserRequest;
import sonmoeum.domain.users.v1.dto.request.UpdateSelfRequest;
import sonmoeum.domain.users.v1.dto.request.UpdateUserRequest;
import sonmoeum.domain.users.v1.dto.response.UserResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@DisplayName("E2E: User 수정 테스트")
class UserUpdateTest extends UserBaseTest {

    @Test
    @DisplayName("관리자 권한으로 User 정보 수정 성공(200 OK)")
    void updateUser_Success() {
        // 먼저 사용자 생성
        CreateUserRequest createReq = new CreateUserRequest(
                "Update Test User",
                "updatetest@test.com",
                "pw_updatetest",
                "updatetest@test.com",
                "010-1111-2222",
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

        // 수정 요청
        UpdateUserRequest updateReq = new UpdateUserRequest(
                "Updated Name",
                "010-9999-8888",
                "updated@test.com",
                "newpassword123!",
                RoleType.ROLE_VOLUNTEER.name()
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
        .when()
            .patch("/{userId}", createdUser.id())
        .then()
            .statusCode(200)
            .body("id", equalTo(createdUser.id().intValue()))
            .body("name", equalTo("Updated Name"))
            .body("phoneNumber", equalTo("010-9999-8888"))
            .body("email", equalTo("updated@test.com"))
            .log().all();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 다른 User 수정 실패(403 Forbidden)")
    void updateUser_Forbidden() {
        UpdateUserRequest updateReq = new UpdateUserRequest(
                "Hacker Name",
                null,
                null,
                null,
                null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
        .when()
            .patch("/{userId}", 1L)
        .then()
            .statusCode(403)
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 User 수정 실패(404 Not Found)")
    void updateUser_NotFound() {
        UpdateUserRequest updateReq = new UpdateUserRequest(
                "New Name",
                null,
                null,
                null,
                null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
        .when()
            .patch("/{userId}", 99999L)
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("본인 정보 수정 성공(200 OK) - volunteer")
    void updateSelf_Success() {
        UpdateSelfRequest updateReq = new UpdateSelfRequest(
                "Updated Volunteer Name",
                "010-5555-6666",
                "volunteer.updated@test.com",
                null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
        .when()
            .patch("/me")
        .then()
            .statusCode(200)
            .body("username", equalTo(TEST_VOLUNTEER_USERNAME))
            .body("name", equalTo("Updated Volunteer Name"))
            .body("phoneNumber", equalTo("010-5555-6666"))
            .body("email", equalTo("volunteer.updated@test.com"))
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 본인 정보 수정 실패(401 Unauthorized)")
    void updateSelf_Unauthorized() {
        UpdateSelfRequest updateReq = new UpdateSelfRequest(
                "Hacker",
                null,
                null,
                null
        );

        given()
            .contentType(ContentType.JSON)
            .body(updateReq)
        .when()
            .patch("/me")
        .then()
            .statusCode(401)
            .log().all();
    }
}
