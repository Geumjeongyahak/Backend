package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.path.json.JsonPath;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;

/** PATCH /subjects/{id}/teacher — 배정·해제와 미래 수업 반영. */
@DisplayName("E2E: 과목 담당 교사 PATCH /teacher 테스트")
public class SubjectTeacherAssignTest extends SubjectBaseTest {

    private static final long CLASSROOM_1 = DEFAULT_CLASSROOM_ID;
    private static final long TEACHER_ID = DEFAULT_TEACHER_ID;
    private static final long NEW_TEACHER_ID = 3L;

    private void createLessonExchangeRequestForTeacher(long teacherId, String lessonDate) {
        createLessonExchangeRequestForTeacher(teacherId, lessonDate, "PENDING");
    }

    private void createLessonExchangeRequestForTeacher(long teacherId, String lessonDate, String status) {
        Long dailyScheduleId = findOrCreateDailySchedule(teacherId, lessonDate);
        jdbcTemplate.update(
            """
            INSERT INTO lesson_exchange_requests (
                id, daily_schedule_id, lesson_date, requested_by, title, classroom_name_snapshot, content, status, expires_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            1L,
            dailyScheduleId,
            lessonDate,
            teacherId,
            "교환 요청",
            "벚꽃반",
            "교환 요청 내용",
            status,
            "2099-02-28 23:59:59"
        );
    }

    private void createLessonExchangeProposalForTeacher(long teacherId, String lessonDate) {
        createLessonExchangeRequestForTeacher(NEW_TEACHER_ID, lessonDate);
        Long dailyScheduleId = findOrCreateDailySchedule(teacherId, lessonDate);
        jdbcTemplate.update(
            """
            INSERT INTO lesson_exchange_proposals (
                id, request_id, proposed_by, proposal_type, daily_schedule_id, lesson_date, content,
                classroom_name_snapshot, status
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            1L,
            1L,
            teacherId,
            "EXCHANGE",
            dailyScheduleId,
            lessonDate,
            "교환 제안 내용",
            "벚꽃반",
            "ACTIVE"
        );
    }

    private Long findOrCreateDailySchedule(long teacherId, String lessonDate) {
        try {
            return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM daily_schedules
                WHERE classroom_id = ? AND lesson_date = ? AND is_deleted = FALSE
                """,
                Long.class,
                CLASSROOM_1,
                Date.valueOf(lessonDate)
            );
        } catch (EmptyResultDataAccessException ignored) {
            jdbcTemplate.update(
                """
                INSERT INTO daily_schedules (classroom_id, teacher_id, lesson_date, status)
                VALUES (?, ?, ?, ?)
                """,
                CLASSROOM_1,
                teacherId,
                Date.valueOf(lessonDate),
                "SCHEDULED"
            );
            return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM daily_schedules
                WHERE classroom_id = ? AND lesson_date = ? AND is_deleted = FALSE
                """,
                Long.class,
                CLASSROOM_1,
                Date.valueOf(lessonDate)
            );
        }
    }

    @Test
    @DisplayName("PATCH /teacher: 담당 교사를 변경하면 미래 수업 교사도 변경된다")
    void assignTeacher_UpdatesFutureLessons() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> request = Map.ofEntries(
            Map.entry("teacherId", NEW_TEACHER_ID)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(200)
            .body("teacherId", is((int) NEW_TEACHER_ID))
            .body("teacherName", is("김철수"))
            .body("teacherAssignedAt", notNullValue());

        Integer changedLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND teacher_id = ?",
            Integer.class,
            subjectId,
            NEW_TEACHER_ID
        );

        assertThat(changedLessonCount).isEqualTo(18);
    }

    @Test
    @DisplayName("PATCH /teacher: 미배정 과목에 교사를 배정하면 미래 수업을 생성한다")
    void assignTeacher_CreatesLessonsForUnassignedSubject() {
        long subjectId = createSubjectWithoutTeacher();

        Map<String, Object> request = Map.ofEntries(
            Map.entry("teacherId", NEW_TEACHER_ID)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(200)
            .body("teacherId", is((int) NEW_TEACHER_ID))
            .body("teacherAssignedAt", notNullValue());

        Integer lessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND teacher_id = ?",
            Integer.class,
            subjectId,
            NEW_TEACHER_ID
        );

        assertThat(lessonCount).isEqualTo(18);
    }

    @Test
    @DisplayName("PATCH /teacher: 기본 분반이 없는 교사를 배정하면 과목 분반으로 채운다")
    void assignTeacher_FillsTeacherDefaultClassroomWhenMissing() {
        jdbcTemplate.update("UPDATE users SET classroom_id = NULL WHERE id = ?", NEW_TEACHER_ID);
        try {
            long subjectId = createSubjectWithoutTeacher();

            Map<String, Object> request = Map.ofEntries(
                Map.entry("teacherId", NEW_TEACHER_ID)
            );

            given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType("application/json")
                .body(request)
                .when()
                .patch("/{subjectId}/teacher", subjectId)
                .then()
                .statusCode(200)
                .body("teacherId", is((int) NEW_TEACHER_ID));

            Long userClassroomId = jdbcTemplate.queryForObject(
                "SELECT classroom_id FROM users WHERE id = ?",
                Long.class,
                NEW_TEACHER_ID
            );
            assertThat(userClassroomId).isEqualTo(CLASSROOM_1);
        } finally {
            jdbcTemplate.update("UPDATE users SET classroom_id = NULL WHERE id = ?", NEW_TEACHER_ID);
        }
    }

    @Test
    @DisplayName("PATCH /teacher: teacherId가 null이면 과목 담당 교사를 비우고 미래 수업을 삭제한다")
    void assignTeacher_ClearsSubjectTeacher() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> request = new HashMap<>();
        request.put("teacherId", null);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(200)
            .body("teacherId", is((Object) null))
            .body("teacherName", is((Object) null))
            .body("teacherAssignedAt", is((Object) null));

        Integer activeLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND is_deleted = FALSE",
            Integer.class,
            subjectId
        );
        Integer deletedLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND is_deleted = TRUE",
            Integer.class,
            subjectId
        );

        assertThat(activeLessonCount).isZero();
        assertThat(deletedLessonCount).isEqualTo(18);
    }

    @Test
    @DisplayName("PATCH /teacher: 담당 교사 해제 후 다시 배정하면 미래 수업을 새로 생성한다")
    void assignTeacher_RecreatesLessonsAfterClearingTeacher() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> clearRequest = new HashMap<>();
        clearRequest.put("teacherId", null);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(clearRequest)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(200);

        Map<String, Object> assignRequest = Map.ofEntries(
            Map.entry("teacherId", NEW_TEACHER_ID)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(assignRequest)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(200)
            .body("teacherId", is((int) NEW_TEACHER_ID))
            .body("teacherAssignedAt", notNullValue());

        Integer activeLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND teacher_id = ? AND is_deleted = FALSE",
            Integer.class,
            subjectId,
            NEW_TEACHER_ID
        );
        Integer totalLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ?",
            Integer.class,
            subjectId
        );

        assertThat(activeLessonCount).isEqualTo(18);
        assertThat(totalLessonCount).isEqualTo(36);
    }

    @Test
    @DisplayName("PATCH /teacher: 미래 수업에 교환 요청이 있으면 담당 교사를 변경할 수 없다")
    void assignTeacher_Conflict_WhenFutureLessonHasExchangeRequest() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);
        createLessonExchangeRequestForTeacher(TEACHER_ID, "2099-03-02");

        Map<String, Object> request = Map.ofEntries(
            Map.entry("teacherId", NEW_TEACHER_ID)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("PATCH /teacher: 미래 수업에 교환 제안이 있으면 담당 교사를 해제할 수 없다")
    void assignTeacher_Conflict_WhenFutureLessonHasExchangeProposal() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);
        createLessonExchangeProposalForTeacher(TEACHER_ID, "2099-03-02");

        Map<String, Object> request = new HashMap<>();
        request.put("teacherId", null);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("PATCH /teacher: 완료된 교환 요청은 담당 교사 변경을 막지 않는다")
    void assignTeacher_Success_WhenFutureLessonHasCompletedExchangeRequest() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);
        createLessonExchangeRequestForTeacher(TEACHER_ID, "2099-03-02", "COMPLETED");

        Map<String, Object> request = Map.ofEntries(
            Map.entry("teacherId", NEW_TEACHER_ID)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/teacher", subjectId)
            .then()
            .statusCode(200)
            .body("teacherId", is((int) NEW_TEACHER_ID));
    }
}
