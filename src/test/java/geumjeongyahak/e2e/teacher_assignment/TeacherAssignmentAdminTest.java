package geumjeongyahak.e2e.teacher_assignment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("teacher-assignment")
@DisplayName("E2E: 관리자 교사 시간표 배정 테스트")
class TeacherAssignmentAdminTest extends BaseE2ETest {

    private static final long UNASSIGNED_SUBJECT_ID = 180L;
    private static final long REPLACEMENT_SUBJECT_ID = 181L;
    private static final long WEEKEND_SUBJECT_ID = 182L;
    private static final long CONFLICT_SUBJECT_ID = 183L;
    private static final long PERMISSION_CLEANUP_SUBJECT_ID = 184L;
    private static final long REJECTED_ABSENCE_SUBJECT_ID = 185L;
    private static final long NON_OVERLAPPING_SUBJECT_ID = 186L;

    private String adminToken;
    private Long testTeacherId;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/admin/teacher-schedule-assignments";
        cleanupFixtures();
        insertSubject(UNASSIGNED_SUBJECT_ID, 2L, null, "임의 배정 과목");
        insertSubject(REPLACEMENT_SUBJECT_ID, 2L, 3L, "교체 배정 과목");
        insertSubject(WEEKEND_SUBJECT_ID, 3L, null, "주말 임의 배정 과목", "SATURDAY", "10:00:00", "11:00:00");
        insertSubject(
            NON_OVERLAPPING_SUBJECT_ID,
            3L,
            null,
            "기간 미중복 주말 임의 배정 과목",
            "SATURDAY",
            "10:00:00",
            "11:00:00",
            "2100-03-02",
            "2100-06-30"
        );
        testTeacherId = userTestHelper.createTestUser("teacher-assignment-target", RoleType.VOLUNTEER).getId();
        insertSubject(CONFLICT_SUBJECT_ID, 2L, null, "충돌 기준 과목");
        insertSubject(PERMISSION_CLEANUP_SUBJECT_ID, 1L, 3L, "권한 정리 과목", "TUESDAY", "19:20:00", "20:00:00");
        insertSubject(REJECTED_ABSENCE_SUBJECT_ID, 2L, 3L, "반려 결강 요청 과목");
        adminToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
    }

    @AfterEach
    void cleanup() {
        cleanupFixtures();
        jdbcTemplate.update("UPDATE users SET classroom_id = NULL WHERE id IN (2, 3)");
        if (testTeacherId != null) {
            jdbcTemplate.update("UPDATE users SET classroom_id = NULL WHERE id = ?", testTeacherId);
            jdbcTemplate.update("DELETE FROM user_permissions WHERE user_id = ?", testTeacherId);
        }
        jdbcTemplate.update("""
            DELETE FROM user_permissions
            WHERE user_id IN (2, 3)
              AND permission_code IN ('channel:write:4', 'channel:write:5', 'channel:write:6')
            """);
    }

    @Test
    @DisplayName("기본 분반이 있는 교원을 다른 분반 과목에 배정해도 기본 분반은 유지된다")
    void assignTeacher_keepsExistingDefaultClassroom() {
        jdbcTemplate.update("UPDATE users SET classroom_id = 1 WHERE id = ?", testTeacherId);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(assignRequest(testTeacherId, UNASSIGNED_SUBJECT_ID))
        .when()
            .patch()
        .then()
            .statusCode(200)
            .body("[0].id", equalTo((int) UNASSIGNED_SUBJECT_ID))
            .body("[0].teacherId", equalTo(testTeacherId.intValue()))
            .body("[0].teacherAssignedAt", notNullValue());

        Long userClassroomId = jdbcTemplate.queryForObject(
            "SELECT classroom_id FROM users WHERE id = ?",
            Long.class,
            testTeacherId
        );
        assertThat(userClassroomId).isEqualTo(1L);

        Integer permissionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_permissions WHERE user_id = ? AND permission_code = 'channel:write:5'",
            Integer.class,
            testTeacherId
        );
        assertThat(permissionCount).isEqualTo(1);
    }

    @Test
    @DisplayName("기본 분반이 없는 교원은 첫 배정 과목 분반이 기본 분반으로 채워진다")
    void assignTeacher_fillsDefaultClassroomWhenMissing() {
        jdbcTemplate.update("UPDATE users SET classroom_id = NULL WHERE id = ?", testTeacherId);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(assignRequest(testTeacherId, UNASSIGNED_SUBJECT_ID))
        .when()
            .patch()
        .then()
            .statusCode(200)
            .body("[0].teacherId", equalTo(testTeacherId.intValue()));

        Long userClassroomId = jdbcTemplate.queryForObject(
            "SELECT classroom_id FROM users WHERE id = ?",
            Long.class,
            testTeacherId
        );
        assertThat(userClassroomId).isEqualTo(2L);
    }

    @Test
    @DisplayName("기존 담당 교사가 있으면 확인 플래그 없이 교체할 수 없다")
    void assignTeacher_existingTeacherWithoutConfirmation_returns409() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(assignRequest(testTeacherId, REPLACEMENT_SUBJECT_ID))
        .when()
            .patch()
        .then()
            .statusCode(409);

        Long teacherId = jdbcTemplate.queryForObject(
            "SELECT teacher_id FROM subjects WHERE id = ?",
            Long.class,
            REPLACEMENT_SUBJECT_ID
        );
        assertThat(teacherId).isEqualTo(3L);
    }

    @Test
    @DisplayName("기존 담당 교사 교체를 확인하면 새 교원으로 변경된다")
    void assignTeacher_existingTeacherWithConfirmation_replacesTeacher() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of(
                "teacherId", testTeacherId,
                "subjectIds", java.util.List.of(REPLACEMENT_SUBJECT_ID),
                "confirmTeacherReplacement", true
            ))
        .when()
            .patch()
        .then()
            .statusCode(200)
            .body("[0].teacherId", equalTo(testTeacherId.intValue()));
    }

    @Test
    @DisplayName("같은 날짜/시간대 기존 Lesson이 있으면 담당 교사 교체 실패 및 롤백")
    void assignTeacher_timeConflict_returns409AndRollsBack() {
        jdbcTemplate.update("UPDATE subjects SET teacher_id = ? WHERE id = ?", testTeacherId, CONFLICT_SUBJECT_ID);
        insertLesson(REPLACEMENT_SUBJECT_ID, 3L, 1800L, "2099-03-02", "19:20:00", "20:00:00");
        insertLesson(CONFLICT_SUBJECT_ID, testTeacherId, 1801L, "2099-03-02", "19:20:00", "20:00:00");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of(
                "teacherId", testTeacherId,
                "subjectIds", java.util.List.of(REPLACEMENT_SUBJECT_ID),
                "confirmTeacherReplacement", true
            ))
        .when()
            .patch()
        .then()
            .statusCode(409);

        Long teacherId = jdbcTemplate.queryForObject(
            "SELECT teacher_id FROM subjects WHERE id = ?",
            Long.class,
            REPLACEMENT_SUBJECT_ID
        );
        assertThat(teacherId).isEqualTo(3L);
    }

    @Test
    @DisplayName("반려된 결강 요청만 있으면 기존 담당 교사를 교체할 수 있다")
    void assignTeacher_rejectedAbsenceRequest_allowsReplacement() {
        insertLesson(REJECTED_ABSENCE_SUBJECT_ID, 3L, 1802L, "2099-03-02", "19:20:00", "20:00:00");
        insertDailyScheduleWithRejectedAbsenceRequest(1900L, REJECTED_ABSENCE_SUBJECT_ID, 3L, "2099-03-02");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of(
                "teacherId", testTeacherId,
                "subjectIds", java.util.List.of(REJECTED_ABSENCE_SUBJECT_ID),
                "confirmTeacherReplacement", true
            ))
        .when()
            .patch()
        .then()
            .statusCode(200)
            .body("[0].teacherId", equalTo(testTeacherId.intValue()));

        Integer updatedLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND teacher_id = ?",
            Integer.class,
            REJECTED_ABSENCE_SUBJECT_ID,
            testTeacherId
        );
        assertThat(updatedLessonCount).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 다른 하루치 일정 과목을 담당 중인 교원은 배정할 수 없다")
    void assignTeacher_differentSchedule_returns409() {
        jdbcTemplate.update("UPDATE users SET classroom_id = 1 WHERE id = ?", testTeacherId);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(assignRequest(testTeacherId, UNASSIGNED_SUBJECT_ID))
        .when()
            .patch()
        .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(assignRequest(testTeacherId, WEEKEND_SUBJECT_ID))
        .when()
            .patch()
        .then()
            .statusCode(409);

        Integer assignedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM subjects WHERE teacher_id = ? AND id IN (?, ?)",
            Integer.class,
            testTeacherId,
            UNASSIGNED_SUBJECT_ID,
            WEEKEND_SUBJECT_ID
        );
        assertThat(assignedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("운영기간이 겹치지 않는 다른 하루치 일정 과목은 배정할 수 있다")
    void assignTeacher_nonOverlappingDifferentSchedule_returns200() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(assignRequest(testTeacherId, UNASSIGNED_SUBJECT_ID))
        .when()
            .patch()
        .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(assignRequest(testTeacherId, NON_OVERLAPPING_SUBJECT_ID))
        .when()
            .patch()
        .then()
            .statusCode(200);

        Integer assignedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM subjects WHERE teacher_id = ? AND id IN (?, ?)",
            Integer.class,
            testTeacherId,
            UNASSIGNED_SUBJECT_ID,
            NON_OVERLAPPING_SUBJECT_ID
        );
        assertThat(assignedCount).isEqualTo(2);
    }

    @Test
    @DisplayName("GET /subjects/me: 로그인 교원의 담당 활성 과목을 조회한다")
    void getMyAssignedSubjects_returnsCurrentTeacherSubjects() {
        given()
            .header(AUTH_HEADER, getAuthHeader(userTestHelper.generateAccessTokenByUserKey("teacher02")))
            .basePath("/api/v1/subjects")
        .when()
            .get("/me")
        .then()
            .statusCode(200)
            .body("id", hasItem((int) REPLACEMENT_SUBJECT_ID));
    }

    @Test
    @DisplayName("담당 교사를 해제하면 Subject와 미래 Lesson이 비워진다")
    void unassignTeacher_clearsSubjectTeacher() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(assignRequest(testTeacherId, UNASSIGNED_SUBJECT_ID))
        .when()
            .patch()
        .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of("subjectIds", java.util.List.of(UNASSIGNED_SUBJECT_ID)))
        .when()
            .delete()
        .then()
            .statusCode(200)
            .body("[0].teacherId", equalTo(null));

        Integer subjectTeacherCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM subjects WHERE id = ? AND teacher_id IS NULL AND teacher_assigned_at IS NULL",
            Integer.class,
            UNASSIGNED_SUBJECT_ID
        );
        assertThat(subjectTeacherCount).isEqualTo(1);

        Long userClassroomId = jdbcTemplate.queryForObject(
            "SELECT classroom_id FROM users WHERE id = ?",
            Long.class,
            testTeacherId
        );
        assertThat(userClassroomId).isNull();
    }

    @Test
    @DisplayName("담당 교사를 해제하고 같은 분반에 남은 담당 과목이 없으면 채널 쓰기 권한도 제거된다")
    void unassignTeacher_removesUnusedClassroomChannelWritePermission() {
        jdbcTemplate.update("""
            INSERT INTO user_permissions (user_id, permission_code)
            VALUES (3, 'channel:write:4')
            """);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of("subjectIds", java.util.List.of(PERMISSION_CLEANUP_SUBJECT_ID)))
        .when()
            .delete()
        .then()
            .statusCode(200)
            .body("[0].teacherId", equalTo(null));

        Integer permissionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_permissions WHERE user_id = 3 AND permission_code = 'channel:write:4'",
            Integer.class
        );
        assertThat(permissionCount).isZero();
    }

    private void insertSubject(long subjectId, long classroomId, Long teacherId, String name) {
        insertSubject(subjectId, classroomId, teacherId, name, "MONDAY", "19:20:00", "20:00:00");
    }

    private void insertSubject(
        long subjectId,
        long classroomId,
        Long teacherId,
        String name,
        String dayOfWeek,
        String startTime,
        String endTime
    ) {
        insertSubject(subjectId, classroomId, teacherId, name, dayOfWeek, startTime, endTime, "2099-03-02", "2099-06-30");
    }

    private void insertSubject(
        long subjectId,
        long classroomId,
        Long teacherId,
        String name,
        String dayOfWeek,
        String startTime,
        String endTime,
        String startAt,
        String endAt
    ) {
        jdbcTemplate.update("""
            INSERT INTO subjects (
                id, class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            VALUES (?, ?, ?, ?, ?, ?, ?,
                    ?, ?, 1, CURRENT_TIMESTAMP, '교사 배정 테스트', TRUE)
            """, subjectId, classroomId, teacherId, name, startAt, endAt, dayOfWeek, startTime, endTime);
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

    private void insertDailyScheduleWithRejectedAbsenceRequest(
        long dailyScheduleId,
        long subjectId,
        long teacherId,
        String lessonDate
    ) {
        Long classroomId = jdbcTemplate.queryForObject(
            "SELECT class_id FROM subjects WHERE id = ?",
            Long.class,
            subjectId
        );
        jdbcTemplate.update("""
            INSERT INTO daily_schedules (
                id, classroom_id, teacher_id, lesson_date, activity_start_time,
                activity_end_time, status, is_deleted
            )
            VALUES (?, ?, ?, ?, TIME '19:20:00', TIME '20:00:00', 'SCHEDULED', FALSE)
            """, dailyScheduleId, classroomId, teacherId, lessonDate);
        jdbcTemplate.update("""
            INSERT INTO absence_requests (
                daily_schedule_id, requested_by, title, reason, expires_at,
                status, approval_at, approval_by, note
            )
            VALUES (?, ?, '반려된 결강 요청', '테스트', TIMESTAMP '2099-03-01 00:00:00',
                    'REJECTED', CURRENT_TIMESTAMP, 1, '반려')
            """, dailyScheduleId, teacherId);
    }

    private void cleanupFixtures() {
        jdbcTemplate.update("""
            DELETE FROM absence_requests
            WHERE daily_schedule_id IN (
                SELECT id FROM daily_schedules WHERE lesson_date >= DATE '2099-01-01'
            )
            """);
        jdbcTemplate.update("""
            DELETE FROM daily_teacher_attendances
            WHERE daily_schedule_id IN (
                SELECT id FROM daily_schedules WHERE lesson_date >= DATE '2099-01-01'
            )
            """);
        jdbcTemplate.update("""
            DELETE FROM daily_student_attendances
            WHERE daily_schedule_id IN (
                SELECT id FROM daily_schedules WHERE lesson_date >= DATE '2099-01-01'
            )
            """);
        jdbcTemplate.update("""
            DELETE FROM daily_schedules WHERE lesson_date >= DATE '2099-01-01'
            """);
        jdbcTemplate.update("""
            DELETE FROM lessons WHERE date >= DATE '2099-01-01'
            """);
        jdbcTemplate.update(
            "DELETE FROM lessons WHERE subject_id IN (?, ?, ?, ?, ?, ?, ?)",
            UNASSIGNED_SUBJECT_ID,
            REPLACEMENT_SUBJECT_ID,
            WEEKEND_SUBJECT_ID,
            CONFLICT_SUBJECT_ID,
            PERMISSION_CLEANUP_SUBJECT_ID,
            REJECTED_ABSENCE_SUBJECT_ID,
            NON_OVERLAPPING_SUBJECT_ID
        );
        jdbcTemplate.update(
            "DELETE FROM subjects WHERE id IN (?, ?, ?, ?, ?, ?, ?)",
            UNASSIGNED_SUBJECT_ID,
            REPLACEMENT_SUBJECT_ID,
            WEEKEND_SUBJECT_ID,
            CONFLICT_SUBJECT_ID,
            PERMISSION_CLEANUP_SUBJECT_ID,
            REJECTED_ABSENCE_SUBJECT_ID,
            NON_OVERLAPPING_SUBJECT_ID
        );
    }

    private Map<String, Object> assignRequest(long teacherId, long subjectId) {
        return Map.of(
            "teacherId", teacherId,
            "subjectIds", java.util.List.of(subjectId)
        );
    }
}
