package geumjeongyahak.e2e.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import geumjeongyahak.domain.auth.enums.RoleType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("E2E: User 조회 테스트")
class UserReadTest extends UserBaseTest {

    private static final long TEACHER_ASSIGNMENT_SUBJECT_ID = 190L;
    private static final long SECOND_TEACHER_ASSIGNMENT_SUBJECT_ID = 191L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanupTeacherAssignmentFixture() {
        jdbcTemplate.update(
            "DELETE FROM subjects WHERE id IN (?, ?)",
            TEACHER_ASSIGNMENT_SUBJECT_ID,
            SECOND_TEACHER_ASSIGNMENT_SUBJECT_ID
        );
    }

    @Test
    @DisplayName("관리자 권한으로 특정 User 조회 성공(200 OK)")
    void getUserById_Success() {
        Long userId = 1L; // admin user from init_data.sql

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", userId)
        .then()
            .statusCode(200)
            .body("id", equalTo(userId.intValue()))
            .body("email", equalTo(TEST_ADMIN_EMAIL))
            .log().all();
    }

    @Test
    @DisplayName("사용자 상세 조회 시 직접 권한과 부서 직책 권한 source를 함께 반환한다(200 OK)")
    void getUserById_IncludesManualAndDepartmentPermissionSources() {
        Long userId = 2L; // teacher01: user_permissions + 교육연구부 MEMBER 권한 보유

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", userId)
        .then()
            .statusCode(200)
            .body("permissions.find { it.code == 'channel:write:1' }.source", equalTo("MANUAL"))
            .body("permissions.find { it.code == 'teacher-application:read:*' }.source", equalTo("MEMBER"))
            .body("permissions.find { it.code == 'daily-schedule:read:*' }.source", equalTo("MEMBER"))
            .log().all();
    }

    @Test
    @DisplayName("존재하지 않는 User 조회 실패(404 Not Found)")
    void getUserById_NotFound() {
        Long nonExistentId = 99999L;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", nonExistentId)
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("비활성화된 User는 상세 조회할 수 없다(404 Not Found)")
    void getUserById_DeactivatedUser() {
        var user = userTestHelper.createTestUser(
            "deactivated-read@test.com",
            "Deactivated Read User",
            "password123!",
            RoleType.GUEST
        );
        jdbcTemplate.update(
            "UPDATE users SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
            user.getId()
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", user.getId())
        .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("비활성화된 User는 사용자 목록에서 제외된다")
    void getUsers_ExcludesDeactivatedUser() {
        var user = userTestHelper.createTestUser(
            "deactivated-list@test.com",
            "Deactivated List User",
            "password123!",
            RoleType.GUEST
        );
        jdbcTemplate.update(
            "UPDATE users SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
            user.getId()
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("name", "Deactivated List User")
            .queryParam("size", 20)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content", empty())
            .body("totalElements", equalTo(0))
            .log().all();
    }

    @Test
    @DisplayName("일반 사용자 권한으로 User 조회 실패(403 Forbidden)")
    void getUserById_Forbidden() {
        Long userId = 1L;

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/{userId}", userId)
        .then()
            .statusCode(403)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 User 조회 실패(401 Unauthorized)")
    void getUserById_Unauthorized() {
        Long userId = 1L;

        given()
        .when()
            .get("/{userId}", userId)
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("본인 정보 조회 성공(200 OK) - admin")
    void getCurrentUser_Success_Admin() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/me")
        .then()
            .statusCode(200)
            .body("email", equalTo(TEST_ADMIN_EMAIL))
            .body("permissions", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("본인 정보 조회 성공(200 OK) - volunteer")
    void getCurrentUser_Success_Volunteer() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
        .when()
            .get("/me")
        .then()
            .statusCode(200)
            .body("email", equalTo(TEST_VOLUNTEER_USERNAME))
            .body("permissions", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 본인 정보 조회 실패(401 Unauthorized)")
    void getCurrentUser_Unauthorized() {
        given()
        .when()
            .get("/me")
        .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("사용자 상세 조회에 교사 담당 과목 목록이 포함된다")
    void getUserById_includesTeacherAssignments() {
        Long volunteerId = getVolunteerUserId();
        insertTeacherAssignmentFixture(volunteerId);
        insertSecondTeacherAssignmentFixture(volunteerId);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .get("/{userId}", volunteerId)
        .then()
            .statusCode(200)
            .body("classroom.id", nullValue())
            .body("teacherAssignments.size()", equalTo(2))
            .body(
                "teacherAssignments.find { it.subjectId == " + TEACHER_ASSIGNMENT_SUBJECT_ID + " }.subjectName",
                equalTo("사용자 응답 배정 과목")
            )
            .body(
                "teacherAssignments.find { it.subjectId == " + SECOND_TEACHER_ASSIGNMENT_SUBJECT_ID + " }.classroomName",
                equalTo("스마트폰반")
            );
    }

    @Test
    @DisplayName("사용자 목록 조회에 교사 담당 과목 요약이 포함된다")
    void getUsers_includesTeacherAssignmentSummary() {
        Long volunteerId = getVolunteerUserId();
        insertTeacherAssignmentFixture(volunteerId);
        insertSecondTeacherAssignmentFixture(volunteerId);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .queryParam("role", "VOLUNTEER")
            .queryParam("size", 50)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("content.find { it.id == " + volunteerId + " }.teacherAssignmentCount", equalTo(2))
            .body("content.find { it.id == " + volunteerId + " }.teacherAssignmentClassroomNames", hasItems("장미반", "스마트폰반"));
    }

    private Long getVolunteerUserId() {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE primary_email = ?",
            Long.class,
            TEST_VOLUNTEER_USERNAME
        );
    }

    private void insertTeacherAssignmentFixture(Long teacherId) {
        jdbcTemplate.update("""
            MERGE INTO subjects (
                id, class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            KEY(id)
            VALUES (
                ?, 2, ?, '사용자 응답 배정 과목', DATE '2099-03-02', DATE '2099-06-30', 'MONDAY',
                TIME '19:20:00', TIME '20:00:00', 1, CURRENT_TIMESTAMP, '사용자 응답 테스트', TRUE
            )
            """, TEACHER_ASSIGNMENT_SUBJECT_ID, teacherId);
    }

    private void insertSecondTeacherAssignmentFixture(Long teacherId) {
        jdbcTemplate.update("""
            MERGE INTO subjects (
                id, class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            KEY(id)
            VALUES (
                ?, 3, ?, '사용자 응답 두 번째 배정 과목', DATE '2099-03-06', DATE '2099-06-30', 'SATURDAY',
                TIME '10:00:00', TIME '11:00:00', 1, CURRENT_TIMESTAMP, '사용자 응답 테스트', TRUE
            )
            """, SECOND_TEACHER_ASSIGNMENT_SUBJECT_ID, teacherId);
    }
}
