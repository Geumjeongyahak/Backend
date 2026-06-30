package geumjeongyahak.e2e.teacher_application;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("teacher-application")
@DisplayName("E2E: 관리자 교원 신청 승인 테스트")
class TeacherApplicationApproveTest extends BaseE2ETest {

    private static final String MANAGE_PERMISSION = "teacher-application:manage:*";
    private static final long ASSIGNED_SUBJECT_ID = 170L;
    private static final long ASSIGNED_SECOND_SUBJECT_ID = 171L;
    private static final long APPROVAL_CONFLICT_SUBJECT_ID = 172L;

    private String adminToken;
    private String guestToken;
    private String managerToken;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/admin/teacher-applications";
        cleanupTeacherApplications();
        cleanupTeacherAssignmentFixture();
        insertTeacherAssignmentFixture();
        resetApplicant();

        adminToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
        guestToken = userTestHelper.generateAccessTokenByUserKey("guest01");

        User manager = userTestHelper.createTestUser("teacher-application-manager", RoleType.GUEST);
        userPermissionRepository.findByUserIdAndPermissionCode(manager.getId(), MANAGE_PERMISSION)
            .orElseGet(() -> userPermissionRepository.save(new UserPermission(manager, MANAGE_PERMISSION)));
        managerToken = userTestHelper.generateAccessTokenByUserKey("teacher-application-manager");
    }

    @AfterEach
    void cleanup() {
        cleanupTeacherApplications();
        cleanupTeacherAssignmentFixture();
        resetApplicant();
    }

    @Test
    @DisplayName("관리자가 PENDING 교원 신청 승인 → 200, 신청자 교원 프로필 승인")
    void approveTeacherApplication_asAdmin_returnsApprovedApplication() {
        insertTeacherApplication(70L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(approveRequest("면접 후 승인"))
            .patch("/{applicationId}/approve", 70L)
            .then()
            .statusCode(200)
            .body("id", equalTo(70))
            .body("status", equalTo("APPROVED"))
            .body("assignedSubjects.subjectId", contains((int) ASSIGNED_SUBJECT_ID, (int) ASSIGNED_SECOND_SUBJECT_ID))
            .body("assignedClassroomId", equalTo(2))
            .body("reviewedAt", notNullValue())
            .body("reviewedByName", equalTo("관리자"))
            .body("reviewNote", equalTo("면접 후 승인"));

        String role = jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = 4", String.class);
        assertThat(role).isEqualTo("VOLUNTEER");
        Map<String, Object> user = jdbcTemplate.queryForMap("""
            SELECT classroom_id, teacher_start_at, teacher_end_at
            FROM users
            WHERE id = 4
            """);
        assertThat(user.get("CLASSROOM_ID")).isEqualTo(2L);
        assertThat(user.get("TEACHER_START_AT").toString()).isEqualTo("2026-06-01");
        assertThat(user.get("TEACHER_END_AT").toString()).isEqualTo("2026-12-31");

        Integer assignedApplicationSubjectCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM teacher_schedule_assignments WHERE teacher_application_id = 70",
            Integer.class
        );
        assertThat(assignedApplicationSubjectCount).isEqualTo(2);

        Integer subjectTeacherCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM subjects WHERE id IN (?, ?) AND teacher_id = 4 AND teacher_assigned_at IS NOT NULL",
            Integer.class,
            ASSIGNED_SUBJECT_ID,
            ASSIGNED_SECOND_SUBJECT_ID
        );
        assertThat(subjectTeacherCount).isEqualTo(2);

        Integer permissionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_permissions WHERE user_id = 4 AND permission_code = 'channel:write:5'",
            Integer.class
        );
        assertThat(permissionCount).isEqualTo(1);
    }

    @Test
    @DisplayName("teacher-application:manage:* 권한으로 교원 신청 승인 → 200")
    void approveTeacherApplication_withManagePermission_returnsApprovedApplication() {
        insertTeacherApplication(71L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(managerToken))
            .contentType("application/json")
            .body(approveRequest("권한 승인"))
            .patch("/{applicationId}/approve", 71L)
            .then()
            .statusCode(200)
            .body("id", equalTo(71))
            .body("status", equalTo("APPROVED"))
            .body("reviewNote", equalTo("권한 승인"));
    }

    @Test
    @DisplayName("권한 없는 사용자가 교원 신청 승인 → 403")
    void approveTeacherApplication_asGuest_returns403() {
        insertTeacherApplication(72L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType("application/json")
            .body(approveRequest("승인"))
            .patch("/{applicationId}/approve", 72L)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("PENDING이 아닌 교원 신청 승인 → 409")
    void approveTeacherApplication_notPending_returns409() {
        insertTeacherApplication(73L, "REJECTED");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(approveRequest("승인"))
            .patch("/{applicationId}/approve", 73L)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("승인 시 신청자가 GUEST가 아니면 → 403")
    void approveTeacherApplication_applicantNotGuest_returns403() {
        insertTeacherApplication(74L, "PENDING");
        jdbcTemplate.update("UPDATE users SET role = 'VOLUNTEER' WHERE id = 4");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(approveRequest("승인"))
            .patch("/{applicationId}/approve", 74L)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 교원 신청 승인 → 404")
    void approveTeacherApplication_notFound_returns404() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(approveRequest("승인"))
            .patch("/{applicationId}/approve", 999_999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("인증 없이 교원 신청 승인 → 401")
    void approveTeacherApplication_unauthenticated_returns401() {
        insertTeacherApplication(75L, "PENDING");

        given()
            .contentType("application/json")
            .body(approveRequest("승인"))
            .patch("/{applicationId}/approve", 75L)
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("교원 활동 종료일이 시작일보다 빠르면 교원 신청 승인 → 400")
    void approveTeacherApplication_invalidPeriod_returns400() {
        insertTeacherApplication(76L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of(
                "assignedSubjectIds", List.of(ASSIGNED_SUBJECT_ID),
                "teacherStartAt", "2026-12-31",
                "teacherEndAt", "2026-06-01",
                "note", "승인"
            ))
            .patch("/{applicationId}/approve", 76L)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("배정 과목 없이 교원 신청 승인 → 400")
    void approveTeacherApplication_withoutAssignedSubject_returns400() {
        insertTeacherApplication(77L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of(
                "teacherStartAt", "2026-06-01",
                "teacherEndAt", "2026-12-31",
                "note", "승인"
            ))
            .patch("/{applicationId}/approve", 77L)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("존재하지 않는 과목으로 교원 신청 승인 → 404")
    void approveTeacherApplication_subjectNotFound_returns404() {
        insertTeacherApplication(78L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of(
                "assignedSubjectIds", List.of(999_999),
                "teacherStartAt", "2026-06-01",
                "teacherEndAt", "2026-12-31",
                "note", "승인"
            ))
            .patch("/{applicationId}/approve", 78L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("승인 중 Lesson 시간 충돌이 발생하면 신청/사용자/과목/권한 변경이 모두 롤백된다")
    void approveTeacherApplication_assignmentConflict_rollsBackAllSideEffects() {
        insertTeacherApplication(79L, "PENDING");
        insertSubject(APPROVAL_CONFLICT_SUBJECT_ID, 1L, 4L, "승인 충돌 기준 과목");
        insertLesson(ASSIGNED_SUBJECT_ID, 3L, 1700L, "2099-03-02", "19:20:00", "20:00:00");
        insertLesson(APPROVAL_CONFLICT_SUBJECT_ID, 4L, 1701L, "2099-03-02", "19:20:00", "20:00:00");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(approveRequest("충돌 승인"))
            .patch("/{applicationId}/approve", 79L)
            .then()
            .statusCode(409);

        Map<String, Object> application = jdbcTemplate.queryForMap("""
            SELECT status, reviewed_by, review_note
            FROM teacher_applications
            WHERE id = 79
            """);
        assertThat(application.get("STATUS")).isEqualTo("PENDING");
        assertThat(application.get("REVIEWED_BY")).isNull();
        assertThat(application.get("REVIEW_NOTE")).isNull();
        Integer assignedApplicationSubjectCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM teacher_schedule_assignments WHERE teacher_application_id = 79",
            Integer.class
        );
        assertThat(assignedApplicationSubjectCount).isZero();

        Map<String, Object> user = jdbcTemplate.queryForMap("""
            SELECT role, classroom_id, teacher_start_at, teacher_end_at
            FROM users
            WHERE id = 4
            """);
        assertThat(user.get("ROLE")).isEqualTo("GUEST");
        assertThat(user.get("CLASSROOM_ID")).isNull();
        assertThat(user.get("TEACHER_START_AT")).isNull();
        assertThat(user.get("TEACHER_END_AT")).isNull();

        Integer subjectTeacherCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM subjects WHERE id = ? AND teacher_id IS NULL AND teacher_assigned_at IS NULL",
            Integer.class,
            ASSIGNED_SUBJECT_ID
        );
        assertThat(subjectTeacherCount).isEqualTo(1);

        Integer permissionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_permissions WHERE user_id = 4 AND permission_code LIKE 'channel:write:%'",
            Integer.class
        );
        assertThat(permissionCount).isZero();
    }

    private void insertTeacherApplication(Long id, String status) {
        jdbcTemplate.update("""
            INSERT INTO teacher_applications (
                id, applicant_id, applicant_name, applicant_phone_number, applicant_email,
                birth_date, address, education_and_major, preferred_subject_id,
                motivation, desired_teacher_image, meaning_of_sharing,
                status, reviewed_at, reviewed_by, review_note, created_at, updated_at
            )
            VALUES (?, 4, '이영희', '010-1234-5678', 'guest01@test.com',
                    '1999-03-15', '부산광역시 금정구', '부산대학교 국어국문학과 졸업', 1,
                    '지원 동기', '희망하는 교사상', '나눔의 의미',
                    ?, NULL, NULL, NULL, ?, ?)
            """,
            id,
            status,
            LocalDateTime.parse("2026-05-20T10:00:00"),
            LocalDateTime.parse("2026-05-20T10:00:00")
        );
    }

    private void cleanupTeacherApplications() {
        jdbcTemplate.update("DELETE FROM teacher_schedule_assignments");
        jdbcTemplate.update("DELETE FROM teacher_applications");
    }

    private void insertTeacherAssignmentFixture() {
        jdbcTemplate.update("""
            MERGE INTO subjects (
                id, class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            KEY(id)
            VALUES (
                ?, 2, NULL, '교원 신청 배정 과목', DATE '2099-03-02', DATE '2099-06-30', 'MONDAY',
                TIME '19:20:00', TIME '20:00:00', 1, NULL, '교원 신청 승인 배정 테스트', TRUE
            )
            """, ASSIGNED_SUBJECT_ID);
        jdbcTemplate.update("""
            MERGE INTO subjects (
                id, class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            KEY(id)
            VALUES (
                ?, 2, NULL, '교원 신청 배정 두 번째 과목', DATE '2099-03-02', DATE '2099-06-30', 'MONDAY',
                TIME '20:10:00', TIME '20:50:00', 2, NULL, '교원 신청 승인 배정 테스트', TRUE
            )
            """, ASSIGNED_SECOND_SUBJECT_ID);
    }

    private void cleanupTeacherAssignmentFixture() {
        jdbcTemplate.update(
            "DELETE FROM lessons WHERE subject_id IN (?, ?, ?)",
            ASSIGNED_SUBJECT_ID,
            ASSIGNED_SECOND_SUBJECT_ID,
            APPROVAL_CONFLICT_SUBJECT_ID
        );
        jdbcTemplate.update(
            "DELETE FROM subjects WHERE id IN (?, ?, ?)",
            ASSIGNED_SUBJECT_ID,
            ASSIGNED_SECOND_SUBJECT_ID,
            APPROVAL_CONFLICT_SUBJECT_ID
        );
    }

    private void insertSubject(long subjectId, long classroomId, long teacherId, String name) {
        jdbcTemplate.update("""
            MERGE INTO subjects (
                id, class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            KEY(id)
            VALUES (
                ?, ?, ?, ?, DATE '2099-03-02', DATE '2099-06-30', 'MONDAY',
                TIME '19:20:00', TIME '20:00:00', 1, CURRENT_TIMESTAMP, '교원 신청 승인 충돌 테스트', TRUE
            )
            """, subjectId, classroomId, teacherId, name);
    }

    private void insertLesson(
        long subjectId,
        long teacherId,
        long lessonId,
        String date,
        String startTime,
        String endTime
    ) {
        jdbcTemplate.update("""
            INSERT INTO lessons (
                id, subject_id, teacher_id, date, start_time, end_time, period, status, is_deleted
            )
            VALUES (?, ?, ?, ?, ?, ?, 1, 'SCHEDULED', FALSE)
            """, lessonId, subjectId, teacherId, date, startTime, endTime);
    }

    private Map<String, Object> approveRequest(String note) {
        return Map.of(
            "assignedSubjectIds", List.of(ASSIGNED_SUBJECT_ID, ASSIGNED_SECOND_SUBJECT_ID),
            "teacherStartAt", "2026-06-01",
            "teacherEndAt", "2026-12-31",
            "note", note
        );
    }

    private void resetApplicant() {
        jdbcTemplate.update("""
            UPDATE users
            SET role = 'GUEST',
                classroom_id = NULL,
                teacher_start_at = NULL,
                teacher_end_at = NULL
            WHERE id = 4
            """);
        jdbcTemplate.update("DELETE FROM user_permissions WHERE user_id = 4 AND permission_code LIKE 'channel:write:%'");
    }
}
