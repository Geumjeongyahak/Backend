package geumjeongyahak.e2e.users;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
import geumjeongyahak.domain.users.repository.UserRepository;
import geumjeongyahak.domain.users.v1.dto.request.CreateUserRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserDetailResponse;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("E2E: User 삭제 테스트")
class UserDeleteTest extends UserBaseTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Test
    @DisplayName("관리자 권한으로 User 비활성화 성공 및 계정 종속 데이터 보존(204 No Content)")
    void deleteUser_Success() {
        CreateUserRequest createReq = new CreateUserRequest(
                "deletetest@test.com",
                "Delete Test User",
                "pw_deletetest",
                "010-3333-4444",
                "GUEST",
                null
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
            .as(UserDetailResponse.class);

        userTestHelper.setUser(createdUser.email());
        var user = userRepository.findById(createdUser.id()).orElseThrow();
        userPermissionRepository.save(new UserPermission(user, "channel:read:*"));

        int credentialCountBefore = userCredentialRepository.findAllByUserId(createdUser.id()).size();
        int permissionCountBefore = userPermissionRepository.findAllByUserId(createdUser.id()).size();

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}", createdUser.id())
        .then()
            .statusCode(204)
            .log().all();

        var deactivatedUser = userRepository.findById(createdUser.id()).orElseThrow();
        assertThat(deactivatedUser.isDeleted()).isTrue();
        assertThat(deactivatedUser.getDeletedAt()).isNotNull();
        assertThat(userCredentialRepository.findAllByUserId(createdUser.id()))
            .hasSize(credentialCountBefore);
        assertThat(userPermissionRepository.findAllByUserId(createdUser.id()))
            .hasSize(permissionCountBefore);
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
    @DisplayName("담당 중인 활성 과목이 있는 User 삭제 실패(409 Conflict)")
    void deleteUser_withActiveTeacherAssignments_returns409() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}", 2L)
        .then()
            .statusCode(409)
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
        CreateUserRequest createReq = new CreateUserRequest(
                "doubledelete@test.com",
                "Double Delete Test",
                "pw_doubledelete",
                "010-5555-6666",
                "GUEST",
                null
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
            .as(UserDetailResponse.class);

        userTestHelper.setUser(createdUser.email());

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}", createdUser.id())
        .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}", createdUser.id())
        .then()
            .statusCode(404)
            .log().all();
    }
}
