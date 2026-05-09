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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("E2E: 과목 PATCH 수정 테스트")
public class SubjectUpdateTest extends SubjectBaseTest {

    private static final long CLASSROOM_1 = 1L;
    private static final long TEACHER_ID = 2L;
    private static final long NEW_TEACHER_ID = 3L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Map<String, Object> createRequest(long classroomId, String name, String dayOfWeek, int period) {
        return Map.ofEntries(
            Map.entry("classroomId", classroomId),
            Map.entry("teacherId", TEACHER_ID),
            Map.entry("name", name),
            Map.entry("startAt", "2099-03-02"),
            Map.entry("endAt", "2099-06-30"),
            Map.entry("dayOfWeek", dayOfWeek),
            Map.entry("startTime", "19:20:00"),
            Map.entry("endTime", "20:00:00"),
            Map.entry("period", period),
            Map.entry("description", "PATCH 테스트")
        );
    }

    private long createSubject(long classroomId, String name, String dayOfWeek, int period) {
        JsonPath created = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(createRequest(classroomId, name, dayOfWeek, period))
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract()
            .jsonPath();

        return created.getLong("id");
    }

    private long createSubjectWithoutTeacher() {
        Map<String, Object> request = new HashMap<>(createRequest(CLASSROOM_1, "미배정 과목", "MONDAY", 2));
        request.remove("teacherId");

        JsonPath created = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("teacherId", is((Object) null))
            .extract()
            .jsonPath();

        return created.getLong("id");
    }

    private void createLessonExchangeRequestForTeacher(long teacherId, String lessonDate) {
        createLessonExchangeRequestForTeacher(teacherId, lessonDate, "PENDING");
    }

    private void createLessonExchangeRequestForTeacher(long teacherId, String lessonDate, String status) {
        jdbcTemplate.update(
            """
            INSERT INTO lesson_exchange_requests (
                id, lesson_date, requested_by, title, classroom_name_snapshot, content, status, scope, expires_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            1L,
            lessonDate,
            teacherId,
            "교환 요청",
            "벚꽃반",
            "교환 요청 내용",
            status,
            "FULL",
            "2099-02-28 23:59:59"
        );
    }

    private void createLessonExchangeProposalForTeacher(long teacherId, String lessonDate) {
        createLessonExchangeRequestForTeacher(NEW_TEACHER_ID, lessonDate);
        jdbcTemplate.update(
            """
            INSERT INTO lesson_exchange_proposals (
                id, request_id, proposed_by, proposal_type, proposal_scope, lesson_date, content,
                classroom_name_snapshot, status
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            1L,
            1L,
            teacherId,
            "EXCHANGE",
            "FULL",
            lessonDate,
            "교환 제안 내용",
            "벚꽃반",
            "ACTIVE"
        );
    }

    @Test
    @DisplayName("PATCH: 기본 정보만 수정 성공(200 OK)")
    void patchSubject_UpdateNameOnly_Success() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> patch = Map.ofEntries(
            Map.entry("name", "국어(수정)"),
            Map.entry("description", "기본 정보 수정")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{subjectId}", subjectId)
            .then()
            .statusCode(200)
            .body("id", is((int) subjectId))
            .body("name", is("국어(수정)"))
            .body("description", is("기본 정보 수정"))
            .body("period", is(2))
            .log().all();
    }

    @Test
    @DisplayName("subject:manage:* 권한으로 과목 수정 성공(200 OK)")
    void patchSubject_Success_WithSubjectManagePermission() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> patch = Map.ofEntries(
            Map.entry("name", "관리 권한 수정")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(subjectManageAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{subjectId}", subjectId)
            .then()
            .statusCode(200)
            .body("id", is((int) subjectId))
            .body("name", is("관리 권한 수정"));
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

    @Test
    @DisplayName("PATCH /schedule: 시간과 교시만 변경하면 미래 수업의 시간과 교시를 수정한다")
    void updateSchedule_UpdatesFutureLessonTimeAndPeriod() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> request = Map.ofEntries(
            Map.entry("startTime", "20:10:00"),
            Map.entry("endTime", "20:50:00"),
            Map.entry("period", 3)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/schedule", subjectId)
            .then()
            .statusCode(200)
            .body("startTime", is("20:10:00"))
            .body("endTime", is("20:50:00"))
            .body("period", is(3));

        Integer updatedLessonCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM lessons
            WHERE subject_id = ? AND is_deleted = FALSE
              AND start_time = '20:10:00' AND end_time = '20:50:00' AND period = 3
            """,
            Integer.class,
            subjectId
        );
        Integer totalLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ?",
            Integer.class,
            subjectId
        );

        assertThat(updatedLessonCount).isEqualTo(18);
        assertThat(totalLessonCount).isEqualTo(18);
    }

    @Test
    @DisplayName("PATCH /schedule: 요일이 변경되면 미래 수업을 삭제 후 새 일정으로 재생성한다")
    void updateSchedule_RecreatesFutureLessonsWhenDayOfWeekChanged() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> request = Map.ofEntries(
            Map.entry("dayOfWeek", "TUESDAY")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/schedule", subjectId)
            .then()
            .statusCode(200)
            .body("dayOfWeek", is("TUESDAY"));

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
        List<LocalDate> activeLessonDates = jdbcTemplate.query(
            "SELECT date FROM lessons WHERE subject_id = ? AND is_deleted = FALSE",
            (rs, rowNum) -> Date.valueOf(rs.getString("date")).toLocalDate(),
            subjectId
        );

        assertThat(activeLessonCount).isEqualTo(18);
        assertThat(deletedLessonCount).isEqualTo(18);
        assertThat(activeLessonDates)
            .isNotEmpty()
            .allMatch(date -> date.getDayOfWeek() == DayOfWeek.TUESDAY);
    }

    @Test
    @DisplayName("PATCH /schedule: 미배정 과목은 일정만 수정하고 수업을 생성하지 않는다")
    void updateSchedule_DoesNotCreateLessonsWhenTeacherIsNotAssigned() {
        long subjectId = createSubjectWithoutTeacher();

        Map<String, Object> request = Map.ofEntries(
            Map.entry("dayOfWeek", "TUESDAY"),
            Map.entry("period", 3)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/schedule", subjectId)
            .then()
            .statusCode(200)
            .body("teacherId", is((Object) null))
            .body("dayOfWeek", is("TUESDAY"))
            .body("period", is(3));

        Integer lessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ?",
            Integer.class,
            subjectId
        );

        assertThat(lessonCount).isZero();
    }

    @Test
    @DisplayName("PATCH /schedule: 운영 기록이 있는 미래 수업은 자동 변경할 수 없다")
    void updateSchedule_Conflict_WhenFutureLessonHasNote() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);
        Long lessonId = jdbcTemplate.queryForObject(
            "SELECT MIN(id) FROM lessons WHERE subject_id = ? AND is_deleted = FALSE",
            Long.class,
            subjectId
        );
        jdbcTemplate.update("UPDATE lessons SET note = ? WHERE id = ?", "운영 기록", lessonId);

        Map<String, Object> request = Map.ofEntries(
            Map.entry("startTime", "20:10:00"),
            Map.entry("endTime", "20:50:00")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/schedule", subjectId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("PATCH /schedule: 변경할 시간이 담당 교사의 기존 수업과 겹치면 409 Conflict")
    void updateSchedule_Conflict_WhenNewTimeOverlapsTeacherLesson() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);
        jdbcTemplate.update(
            """
            INSERT INTO subjects (
                id, class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)
            """,
            100L,
            CLASSROOM_1,
            TEACHER_ID,
            "충돌 과목",
            "2099-03-02",
            "2099-06-30",
            "MONDAY",
            "20:10:00",
            "20:50:00",
            3,
            "충돌 검증용",
            true
        );
        jdbcTemplate.update(
            """
            INSERT INTO lessons (
                id, subject_id, teacher_id, date, start_time, end_time, period, status, teacher_attendance, is_deleted
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            100L,
            100L,
            TEACHER_ID,
            "2099-03-02",
            "20:10:00",
            "20:50:00",
            3,
            "SCHEDULED",
            "ABSENT",
            false
        );

        Map<String, Object> request = Map.ofEntries(
            Map.entry("startTime", "20:10:00"),
            Map.entry("endTime", "20:50:00"),
            Map.entry("period", 3)
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch("/{subjectId}/schedule", subjectId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("PATCH: 과목명이 공백이면 400 Bad Request")
    void patchSubject_BadRequest_WhenNameIsBlank() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> patch = Map.ofEntries(
            Map.entry("name", "   ")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{subjectId}", subjectId)
            .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("PATCH: 일정 필드는 기본 정보 수정 대상이 아니므로 변경되지 않는다")
    void patchSubject_IgnoresScheduleFields() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> patch = Map.ofEntries(
            Map.entry("dayOfWeek", "MONDAY"),
            Map.entry("period", 2),
            Map.entry("startAt", "2099-05-01"),
            Map.entry("endAt", "2099-07-01")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{subjectId}", subjectId)
            .then()
            .statusCode(200)
            .body("dayOfWeek", is("MONDAY"))
            .body("period", is(2))
            .body("startAt", is("2099-03-02"))
            .body("endAt", is("2099-06-30"))
            .log().all();
    }

    @Test
    @DisplayName("PATCH: 권한 없는 사용자 수정 실패(403 Forbidden)")
    void patchSubject_Forbidden_Volunteer() {
        long subjectId = createSubject(CLASSROOM_1, "국어", "MONDAY", 2);

        Map<String, Object> patch = Map.ofEntries(
            Map.entry("name", "수정시도")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{subjectId}", subjectId)
            .then()
            .statusCode(403)
            .log().all();
    }
}
