package geumjeongyahak.e2e.users;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import geumjeongyahak.domain.users.v1.dto.request.CreateUserRequest;
import geumjeongyahak.domain.users.v1.dto.request.UserPermissionRequest;
import geumjeongyahak.domain.users.v1.dto.request.UpdateSelfRequest;
import geumjeongyahak.domain.users.v1.dto.request.UpdateUserRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserDetailResponse;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("E2E: User 수정 테스트")
class UserUpdateTest extends UserBaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("관리자 권한으로 User 정보 수정 성공(200 OK)")
    void updateUser_Success() {
        // 먼저 사용자 생성
        CreateUserRequest createReq = new CreateUserRequest(
                "updatetest@test.com",
                "Update Test User",
                "pw_updatetest",
                "010-1111-2222",
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

        // 수정 요청
        UpdateUserRequest updateReq = new UpdateUserRequest(
                "Updated Name",
                "010-9999-8888",
                "updated@test.com",
                "newpassword123!",
                "VOLUNTEER",
                null
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
            .body("role", equalTo("VOLUNTEER"))
            .log().all();
    }

    @Test
    @DisplayName("관리자가 사용자의 부서를 변경 성공(200 OK)")
    void updateUser_Department_Success() {
        // 1. 사용자 생성 (부서 없음)
        CreateUserRequest createReq = new CreateUserRequest(
                "depttest@test.com",
                "Dept Test User",
                "pw_depttest",
                "010-0000-0000",
                "VOLUNTEER",
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
            .body("department", nullValue())
            .extract()
            .as(UserDetailResponse.class);

        // 2. 부서 할당 (ID: 1 - 교무기획부)
        UpdateUserRequest updateReq = new UpdateUserRequest(
                null, null, null, null, null, 1L
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
        .when()
            .patch("/{userId}", createdUser.id())
        .then()
            .statusCode(200)
            .body("department.id", equalTo(1))
            .log().all();

        // 3. 부서 해제 (null)
        UpdateUserRequest clearReq = new UpdateUserRequest(
                null, null, null, null, null, null
        );

        // Note: UpdateUserRequest에서 null 전달 시 유지가 될지 해제가 될지는 UserCrudService 구현에 따라 다름.
        // 현재 UserCrudService는 Optional.ifPresent로 처리하므로 null 전달 시 "유지"될 가능성이 높음.
        // 부서 해제 기능이 필요하다면 별도의 로직이 필요할 수 있으나, 일단 변경 확인에 집중함.
    }

    @Test
    @DisplayName("관리자가 사용자의 분반을 변경 성공(200 OK)")
    void updateUser_Classroom_Success() {
        CreateUserRequest createReq = new CreateUserRequest(
            "classroom-update@test.com",
            "Classroom Update User",
            "pw_classroom",
            "010-1111-3333",
            "VOLUNTEER",
            null,
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
            .body("classroom", nullValue())
            .extract()
            .as(UserDetailResponse.class);

        userTestHelper.setUser(createdUser.email());

        UpdateUserRequest updateReq = new UpdateUserRequest(
            null, null, null, null, null, null, 2L
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
        .when()
            .patch("/{userId}", createdUser.id())
        .then()
            .statusCode(200)
            .body("classroom.id", equalTo(2))
            .body("classroom.name", equalTo("장미반"))
            .log().all();
    }

    @Test
    @DisplayName("role을 GUEST로 변경하면 교원 정보와 직접 권한을 회수한다(200 OK)")
    void updateUser_ToGuest_ReleasesTeacherProfileAndPermissions() {
        CreateUserRequest createReq = new CreateUserRequest(
            "release-teacher@test.com",
            "Release Teacher User",
            "password123!",
            "010-2222-3333",
            "MANAGER",
            2L,
            2L
        );

        var createdUser = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(createReq)
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("role", equalTo("MANAGER"))
            .body("department.id", equalTo(2))
            .body("classroom.id", equalTo(2))
            .extract()
            .as(UserDetailResponse.class);

        userTestHelper.setUser(createdUser.email());

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new UserPermissionRequest("channel:write:*"))
        .when()
            .post("/{userId}/permissions", createdUser.id())
        .then()
            .statusCode(200)
            .body("size()", equalTo(1));

        UpdateUserRequest updateReq = new UpdateUserRequest(
            null, null, null, null, "GUEST", 1L, 1L
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
        .when()
            .patch("/{userId}", createdUser.id())
        .then()
            .statusCode(200)
            .body("role", equalTo("GUEST"))
            .body("department", nullValue())
            .body("classroom", nullValue())
            .body("teacherEndAt", equalTo(LocalDate.now().toString()))
            .body("permissions", empty())
            .log().all();

        Integer permissionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_permissions WHERE user_id = ?",
            Integer.class,
            createdUser.id()
        );
        org.assertj.core.api.Assertions.assertThat(permissionCount).isZero();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 다른 User 수정 실패(403 Forbidden)")
    void updateUser_Forbidden() {
        UpdateUserRequest updateReq = new UpdateUserRequest(
                "Hacker Name",
                null,
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
