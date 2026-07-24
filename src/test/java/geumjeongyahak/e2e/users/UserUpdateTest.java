package geumjeongyahak.e2e.users;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import geumjeongyahak.domain.users.v1.dto.request.AssignUserClassroomRequest;
import geumjeongyahak.domain.users.v1.dto.request.CreateUserRequest;
import geumjeongyahak.domain.users.v1.dto.request.UserPermissionRequest;
import geumjeongyahak.domain.users.v1.dto.request.UpdateSelfRequest;
import geumjeongyahak.domain.users.v1.dto.request.UpdateUserRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserDetailResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("E2E: User 수정 테스트")
class UserUpdateTest extends UserBaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("같은 부서의 두 번째 사용자를 MANAGER로 변경할 수 없다(409 Conflict)")
    void updateUser_ToDuplicateDepartmentManager_Conflict() {
        var manager = createUser(
            "update-manager-existing@test.com",
            "기존 부서장",
            "MANAGER",
            3L
        );
        var volunteer = createUser(
            "update-manager-target@test.com",
            "부서장 변경 대상",
            "VOLUNTEER",
            3L
        );

        UpdateUserRequest updateRequest = new UpdateUserRequest(
            null, null, null, null, null, "MANAGER", null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateRequest)
        .when()
            .patch("/{userId}", volunteer.id())
        .then()
            .statusCode(409)
            .body("code", equalTo("BIZ-01-010"));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", manager.id())
        .then()
            .statusCode(200)
            .body("role", equalTo("MANAGER"))
            .body("department.id", equalTo(3));
    }

    @Test
    @DisplayName("MANAGER를 이미 부서장이 있는 부서로 이동할 수 없다(409 Conflict)")
    void updateUser_MoveManagerToOccupiedDepartment_Conflict() {
        createUser(
            "move-manager-existing@test.com",
            "이동 대상 부서의 부서장",
            "MANAGER",
            3L
        );
        var movingManager = createUser(
            "move-manager-target@test.com",
            "이동할 부서장",
            "MANAGER",
            5L
        );

        UpdateUserRequest updateRequest = new UpdateUserRequest(
            null, null, null, null, null, null, 3L
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateRequest)
        .when()
            .patch("/{userId}", movingManager.id())
        .then()
            .statusCode(409)
            .body("code", equalTo("BIZ-01-010"));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", movingManager.id())
        .then()
            .statusCode(200)
            .body("role", equalTo("MANAGER"))
            .body("department.id", equalTo(5));
    }

    @Test
    @DisplayName("기존 MANAGER가 역할을 변경하면 같은 부서에 새 MANAGER를 지정할 수 있다")
    void updateUser_ChangeManagerRole_ReleasesDepartmentManagerPosition() {
        var previousManager = createUser(
            "release-manager-existing@test.com",
            "기존 부서장",
            "MANAGER",
            3L
        );
        var nextManager = createUser(
            "release-manager-target@test.com",
            "새 부서장",
            "VOLUNTEER",
            3L
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new UpdateUserRequest(
                null, null, null, null, null, "VOLUNTEER", null
            ))
        .when()
            .patch("/{userId}", previousManager.id())
        .then()
            .statusCode(200)
            .body("role", equalTo("VOLUNTEER"));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new UpdateUserRequest(
                null, null, null, null, null, "MANAGER", null
            ))
        .when()
            .patch("/{userId}", nextManager.id())
        .then()
            .statusCode(200)
            .body("role", equalTo("MANAGER"))
            .body("department.id", equalTo(3));
    }

    @Test
    @DisplayName("1. 삭제된 MANAGER는 부서장 계산에서 제외한다")
    void departmentManager_DeactivatedManagerDoesNotBlockReplacement() {
        var previousManager = createUser(
            "deactivated-manager-existing@test.com",
            "삭제할 부서장",
            "MANAGER",
            3L
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}", previousManager.id())
        .then()
            .statusCode(204);

        UserDetailResponse replacement = createUser(
            "deactivated-manager-replacement@test.com",
            "후임 부서장",
            "MANAGER",
            3L
        );
        org.assertj.core.api.Assertions.assertThat(replacement.role()).isEqualTo("MANAGER");
        org.assertj.core.api.Assertions.assertThat(replacement.department().id()).isEqualTo(3L);
    }

    @Test
    @DisplayName("2. 부서가 해제된 MANAGER는 기존 부서의 부서장 계산에서 제외한다")
    void departmentManager_ReleasedDepartmentDoesNotBlockReplacement() {
        var previousManager = createUser(
            "released-department-manager@test.com",
            "부서를 해제할 부서장",
            "MANAGER",
            3L
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}/department", previousManager.id())
        .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", previousManager.id())
        .then()
            .statusCode(200)
            .body("role", equalTo("MANAGER"))
            .body("department", nullValue());

        UserDetailResponse replacement = createUser(
            "released-department-replacement@test.com",
            "후임 부서장",
            "MANAGER",
            3L
        );
        org.assertj.core.api.Assertions.assertThat(replacement.department().id()).isEqualTo(3L);
    }

    @Test
    @DisplayName("3. 부서가 없는 MANAGER는 여러 명 생성할 수 있다")
    void departmentManager_MultipleManagersWithoutDepartmentAreAllowed() {
        UserDetailResponse firstManager = createUser(
            "manager-without-department-first@test.com",
            "미소속 부서장 1",
            "MANAGER",
            null
        );
        UserDetailResponse secondManager = createUser(
            "manager-without-department-second@test.com",
            "미소속 부서장 2",
            "MANAGER",
            null
        );

        org.assertj.core.api.Assertions.assertThat(firstManager.role()).isEqualTo("MANAGER");
        org.assertj.core.api.Assertions.assertThat(firstManager.department()).isNull();
        org.assertj.core.api.Assertions.assertThat(secondManager.role()).isEqualTo("MANAGER");
        org.assertj.core.api.Assertions.assertThat(secondManager.department()).isNull();
    }

    @Test
    @DisplayName("4. 미소속 MANAGER를 이미 부서장이 있는 부서에 배정할 수 없다")
    void departmentManager_AssigningUnassignedManagerToOccupiedDepartmentFails() {
        createUser(
            "assign-unassigned-manager-existing@test.com",
            "기존 부서장",
            "MANAGER",
            3L
        );
        var unassignedManager = createUser(
            "assign-unassigned-manager-target@test.com",
            "미소속 부서장",
            "MANAGER",
            null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new UpdateUserRequest(
                null, null, null, null, null, null, 3L
            ))
        .when()
            .patch("/{userId}", unassignedManager.id())
        .then()
            .statusCode(409)
            .body("code", equalTo("BIZ-01-010"));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", unassignedManager.id())
        .then()
            .statusCode(200)
            .body("role", equalTo("MANAGER"))
            .body("department", nullValue());
    }

    @Test
    @DisplayName("5. MANAGER가 빈 부서로 이동하면 기존 부서에 후임을 지정할 수 있다")
    void departmentManager_MoveToEmptyDepartmentReleasesPreviousDepartment() {
        var movingManager = createUser(
            "move-to-empty-department-manager@test.com",
            "이동할 부서장",
            "MANAGER",
            3L
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new UpdateUserRequest(
                null, null, null, null, null, null, 5L
            ))
        .when()
            .patch("/{userId}", movingManager.id())
        .then()
            .statusCode(200)
            .body("role", equalTo("MANAGER"))
            .body("department.id", equalTo(5));

        UserDetailResponse replacement = createUser(
            "move-to-empty-department-replacement@test.com",
            "기존 부서 후임",
            "MANAGER",
            3L
        );
        org.assertj.core.api.Assertions.assertThat(replacement.department().id()).isEqualTo(3L);
    }

    @Test
    @DisplayName("6. 역할과 부서를 동시에 변경해도 최종 상태를 기준으로 검증한다")
    void departmentManager_ChangingRoleAndDepartmentTogetherUsesFinalState() {
        var successfulTarget = createUser(
            "combined-manager-success@test.com",
            "동시 변경 성공 대상",
            "VOLUNTEER",
            null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new UpdateUserRequest(
                null, null, null, null, null, "MANAGER", 3L
            ))
        .when()
            .patch("/{userId}", successfulTarget.id())
        .then()
            .statusCode(200)
            .body("role", equalTo("MANAGER"))
            .body("department.id", equalTo(3));

        createUser(
            "combined-manager-existing@test.com",
            "다른 부서의 기존 부서장",
            "MANAGER",
            5L
        );
        var conflictingTarget = createUser(
            "combined-manager-conflict@test.com",
            "동시 변경 충돌 대상",
            "VOLUNTEER",
            null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new UpdateUserRequest(
                null, null, null, null, null, "MANAGER", 5L
            ))
        .when()
            .patch("/{userId}", conflictingTarget.id())
        .then()
            .statusCode(409)
            .body("code", equalTo("BIZ-01-010"));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", conflictingTarget.id())
        .then()
            .statusCode(200)
            .body("role", equalTo("VOLUNTEER"))
            .body("department", nullValue());
    }

    @Test
    @DisplayName("7. 기존 MANAGER에게 같은 부서를 다시 지정할 수 있다")
    void departmentManager_ReassigningSameDepartmentToSelfSucceeds() {
        var manager = createUser(
            "same-department-manager@test.com",
            "동일 부서 재지정 대상",
            "MANAGER",
            3L
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new UpdateUserRequest(
                null, null, null, null, null, null, 3L
            ))
        .when()
            .patch("/{userId}", manager.id())
        .then()
            .statusCode(200)
            .body("role", equalTo("MANAGER"))
            .body("department.id", equalTo(3));
    }

    @Test
    @DisplayName("8. 같은 부서에 일반 사용자는 여러 명 생성할 수 있다")
    void departmentManager_MultipleNonManagersInSameDepartmentAreAllowed() {
        UserDetailResponse firstVolunteer = createUser(
            "same-department-volunteer-first@test.com",
            "같은 부서 봉사자 1",
            "VOLUNTEER",
            3L
        );
        UserDetailResponse secondVolunteer = createUser(
            "same-department-volunteer-second@test.com",
            "같은 부서 봉사자 2",
            "VOLUNTEER",
            3L
        );

        org.assertj.core.api.Assertions.assertThat(firstVolunteer.department().id()).isEqualTo(3L);
        org.assertj.core.api.Assertions.assertThat(secondVolunteer.department().id()).isEqualTo(3L);
    }

    @Test
    @DisplayName("9. MANAGER 중복 충돌 시 함께 요청한 다른 필드도 변경되지 않는다")
    void departmentManager_ConflictRollsBackAllRequestedChanges() {
        createUser(
            "rollback-manager-existing@test.com",
            "기존 부서장",
            "MANAGER",
            3L
        );
        var target = createUser(
            "rollback-manager-target@test.com",
            "변경 전 이름",
            "VOLUNTEER",
            3L
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new UpdateUserRequest(
                "변경 후 이름",
                "010-9999-9999",
                null,
                "rollback-manager-changed@test.com",
                null,
                "MANAGER",
                3L
            ))
        .when()
            .patch("/{userId}", target.id())
        .then()
            .statusCode(409)
            .body("code", equalTo("BIZ-01-010"));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", target.id())
        .then()
            .statusCode(200)
            .body("name", equalTo("변경 전 이름"))
            .body("phoneNumber", equalTo("010-2222-0000"))
            .body("email", equalTo("rollback-manager-target@test.com"))
            .body("role", equalTo("VOLUNTEER"))
            .body("department.id", equalTo(3));
    }

    @Test
    @DisplayName("10. 같은 부서의 MANAGER 동시 생성 요청 중 하나만 성공한다")
    void departmentManager_ConcurrentCreationAllowsExactlyOneManager() throws Exception {
        String firstEmail = "concurrent-manager-first@test.com";
        String secondEmail = "concurrent-manager-second@test.com";
        Long departmentId = 6L;
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> firstResponse = executor.submit(concurrentManagerCreation(
                firstEmail,
                "동시 생성 부서장 1",
                departmentId,
                ready,
                start
            ));
            Future<Integer> secondResponse = executor.submit(concurrentManagerCreation(
                secondEmail,
                "동시 생성 부서장 2",
                departmentId,
                ready,
                start
            ));

            org.assertj.core.api.Assertions.assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statusCodes = List.of(
                firstResponse.get(30, TimeUnit.SECONDS),
                secondResponse.get(30, TimeUnit.SECONDS)
            );
            org.assertj.core.api.Assertions.assertThat(statusCodes)
                .containsExactlyInAnyOrder(201, 409);

            Integer activeManagerCount = jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM users
                    WHERE department_id = ?
                      AND role = 'MANAGER'
                      AND is_deleted = FALSE
                    """,
                Integer.class,
                departmentId
            );
            org.assertj.core.api.Assertions.assertThat(activeManagerCount).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
            userTestHelper.setUser(firstEmail);
            userTestHelper.setUser(secondEmail);
        }
    }

    @Test
    @DisplayName("관리자 권한으로 User 정보 수정 성공(200 OK)")
    void updateUser_Success() {
        // 먼저 사용자 생성
        CreateUserRequest createReq = new CreateUserRequest(
                "updatetest@test.com",
                "Update Test User",
                "pw_updatetest",
                "010-1111-2222",
                DEFAULT_BIRTH_DATE,
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
                LocalDate.of(2000, 2, 2),
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
            .body("birthDate", equalTo("2000-02-02"))
            .body("role", equalTo("VOLUNTEER"))
            .log().all();

        org.assertj.core.api.Assertions.assertThat(
            userTestHelper.getUser("updated@test.com").getResidentRegistrationNumberPrefix()
        ).isEqualTo("000202");
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
                DEFAULT_BIRTH_DATE,
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
                null, null, null, null, null, null, 1L
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

        // 3. 부서 해제
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}/department", createdUser.id())
        .then()
            .statusCode(204)
            .log().all();

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", createdUser.id())
        .then()
            .statusCode(200)
            .body("department", nullValue())
            .log().all();
    }

    @Test
    @DisplayName("관리자가 사용자의 분반을 변경 성공(200 OK)")
    void updateUser_Classroom_Success() {
        CreateUserRequest createReq = new CreateUserRequest(
            "classroom-update@test.com",
            "Classroom Update User",
            "pw_classroom",
            "010-1111-3333",
            DEFAULT_BIRTH_DATE,
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
            null, null, null, null, null, null, null, 2L
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
    @DisplayName("관리자가 User 대표 분반을 지정, 변경, 해제 성공")
    void assignAndReleaseUserClassroom_Success() {
        CreateUserRequest createReq = new CreateUserRequest(
            "classroom-admin@test.com",
            "Classroom Admin User",
            "pw_classroom_admin",
            "010-1111-4444",
            DEFAULT_BIRTH_DATE,
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

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new AssignUserClassroomRequest(1L))
        .when()
            .put("/{userId}/classroom", createdUser.id())
        .then()
            .statusCode(200)
            .body("id", equalTo(createdUser.id().intValue()))
            .body("classroom.id", equalTo(1))
            .body("classroom.name", equalTo("벚꽃반"))
            .log().all();

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new AssignUserClassroomRequest(2L))
        .when()
            .put("/{userId}/classroom", createdUser.id())
        .then()
            .statusCode(200)
            .body("id", equalTo(createdUser.id().intValue()))
            .body("classroom.id", equalTo(2))
            .body("classroom.name", equalTo("장미반"))
            .log().all();

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{userId}/classroom", createdUser.id())
        .then()
            .statusCode(204)
            .log().all();

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", createdUser.id())
        .then()
            .statusCode(200)
            .body("classroom", nullValue())
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
            DEFAULT_BIRTH_DATE,
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
            null, null, null, null, null, "GUEST", 1L, 1L
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
    @DisplayName("관리자가 GUEST를 교원 역할로 변경하면 활동 시작일을 오늘로 설정한다")
    void updateUser_FromGuestToTeacherRole_SetsTeacherStartAt() {
        CreateUserRequest createReq = new CreateUserRequest(
            "promote-teacher@test.com",
            "Promote Teacher User",
            "password123!",
            "010-7777-8888",
            DEFAULT_BIRTH_DATE,
            "GUEST",
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
            .body("role", equalTo("GUEST"))
            .body("teacherStartAt", nullValue())
            .extract()
            .as(UserDetailResponse.class);

        userTestHelper.setUser(createdUser.email());

        UpdateUserRequest updateReq = new UpdateUserRequest(
            null, null, null, null, null, "MANAGER", null, null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
        .when()
            .patch("/{userId}", createdUser.id())
        .then()
            .statusCode(200)
            .body("role", equalTo("MANAGER"))
            .body("teacherStartAt", equalTo(LocalDate.now().toString()))
            .body("teacherEndAt", nullValue());
    }

    @Test
    @DisplayName("담당 중인 활성 과목이 있는 User는 교사 배정 불가 역할로 변경 실패(409 Conflict)")
    void updateUser_withActiveTeacherAssignmentsToGuest_returns409() {
        UpdateUserRequest updateReq = new UpdateUserRequest(
            null, null, null, null, null, "GUEST", null, null
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(updateReq)
        .when()
            .patch("/{userId}", 2L)
        .then()
            .statusCode(409)
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
                LocalDate.of(2001, 3, 3),
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
            .body("birthDate", equalTo("2001-03-03"))
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 본인 정보 수정 실패(401 Unauthorized)")
    void updateSelf_Unauthorized() {
        UpdateSelfRequest updateReq = new UpdateSelfRequest(
                "Hacker",
                null,
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

    private UserDetailResponse createUser(
        String email,
        String name,
        String role,
        Long departmentId
    ) {
        UserDetailResponse user = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new CreateUserRequest(
                email,
                name,
                "password123!",
                "010-2222-0000",
                DEFAULT_BIRTH_DATE,
                role,
                departmentId
            ))
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .as(UserDetailResponse.class);
        userTestHelper.setUser(email);
        return user;
    }

    private Callable<Integer> concurrentManagerCreation(
        String email,
        String name,
        Long departmentId,
        CountDownLatch ready,
        CountDownLatch start
    ) {
        return () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 생성 요청 시작 신호를 받지 못했습니다.");
            }
            return given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType(ContentType.JSON)
                .body(new CreateUserRequest(
                    email,
                    name,
                    "password123!",
                    "010-3333-0000",
                    DEFAULT_BIRTH_DATE,
                    "MANAGER",
                    departmentId
                ))
            .when()
                .post()
            .then()
                .extract()
                .statusCode();
        };
    }
}
