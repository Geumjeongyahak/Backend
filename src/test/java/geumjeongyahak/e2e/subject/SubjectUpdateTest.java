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
    @DisplayName("PATCH /schedule: 기간 교집합에 실제 수업 요일이 없으면 수정할 수 있다")
    void updateSchedule_Success_WhenOverlapHasNoActualLessonDay() {
        Map<String, Object> targetRequest = new HashMap<>(
            createRequest(CLASSROOM_1, "상반기 토요일 과목", "SATURDAY", 1)
        );
        targetRequest.remove("teacherId");
        targetRequest.put("startAt", "2026-02-01");
        targetRequest.put("endAt", "2026-06-30");
        targetRequest.put("startTime", "10:00:00");
        targetRequest.put("endTime", "10:40:00");

        long subjectId = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(targetRequest)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        jdbcTemplate.update(
            """
            INSERT INTO subjects (
                class_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, description, is_active
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            CLASSROOM_1,
            "하반기 토요일 과목",
            "2026-06-29",
            "2026-09-30",
            "SATURDAY",
            "10:00:00",
            "10:40:00",
            1,
            "실제 수업일 검증용",
            true
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of("endTime", "10:50:00"))
        .when()
            .patch("/{subjectId}/schedule", subjectId)
        .then()
            .statusCode(200)
            .body("endTime", is("10:50:00"));
    }

    @Test
    @DisplayName("PATCH /schedule: 다른 교시라도 실제 수업 시간이 겹치면 409 Conflict")
    void updateSchedule_Conflict_WhenDifferentPeriodsActuallyOverlap() {
        createSubject(CLASSROOM_1, "기준 과목", "MONDAY", 2);

        Map<String, Object> targetRequest = new HashMap<>(
            createRequest(CLASSROOM_1, "수정 대상 과목", "MONDAY", 3)
        );
        targetRequest.remove("teacherId");
        targetRequest.put("startTime", "20:00:00");
        targetRequest.put("endTime", "20:40:00");

        long targetSubjectId = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(targetRequest)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of(
                "startTime", "19:50:00",
                "endTime", "20:30:00",
                "period", 3
            ))
        .when()
            .patch("/{subjectId}/schedule", targetSubjectId)
        .then()
            .statusCode(409)
            .body("code", is("BIZ-05-001"));
    }

    @Test
    @DisplayName("PATCH /schedule: 동일한 일정으로 수정해도 자기 자신과 충돌하지 않는다")
    void updateSchedule_Success_WhenOnlyCandidateIsItself() {
        long subjectId = createSubjectWithoutTeacher();

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of(
                "startAt", "2099-03-02",
                "endAt", "2099-06-30",
                "dayOfWeek", "MONDAY",
                "startTime", "19:20:00",
                "endTime", "20:00:00",
                "period", 2
            ))
        .when()
            .patch("/{subjectId}/schedule", subjectId)
        .then()
            .statusCode(200)
            .body("id", is((int) subjectId))
            .body("startTime", is("19:20:00"))
            .body("endTime", is("20:00:00"));
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
                id, subject_id, teacher_id, date, start_time, end_time, period, status, is_deleted
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            100L,
            100L,
            TEACHER_ID,
            "2099-03-02",
            "20:10:00",
            "20:50:00",
            3,
            "SCHEDULED",
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

    private static final long CLASSROOM_2 = 2L;
    private static final String TERM_START = "2099-03-02";
    private static final String TERM_END = "2099-06-30";
    private static final int MONDAYS_IN_TERM = 18;

    private long createScheduledSubject(
        long classroomId,
        Long teacherId,
        String name,
        String startAt,
        String endAt,
        String startTime,
        String endTime,
        int period
    ) {
        Map<String, Object> request = new HashMap<>(createRequest(classroomId, name, "MONDAY", period));
        if (teacherId == null) {
            request.remove("teacherId");
        } else {
            request.put("teacherId", teacherId);
        }
        request.put("startAt", startAt);
        request.put("endAt", endAt);
        request.put("startTime", startTime);
        request.put("endTime", endTime);

        return given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
        .when()
            .post()
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    private int countActiveLessons(long subjectId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND is_deleted = FALSE",
            Integer.class,
            subjectId
        );
    }

    /** 교사 TEACHER_ID의 월요일 2교시 12:20–13:00 수업을 학기 전체에 만든다. */
    private void createSecondPeriodOfTeacher() {
        long secondPeriod = createScheduledSubject(
            CLASSROOM_1, TEACHER_ID, "2교시", TERM_START, TERM_END, "12:20:00", "13:00:00", 2
        );
        assertThat(countActiveLessons(secondPeriod)).isEqualTo(MONDAYS_IN_TERM);
    }

    @Test
    @DisplayName("PATCH /schedule: 기간을 바꾸며 같은 교사의 앞 교시 종료 시각에 맞닿게 옮기면 수업을 재생성한다")
    void updateSchedule_Success_WhenRecreatedLessonsTouchTeacherLessonBoundary() {
        createSecondPeriodOfTeacher();
        long thirdPeriod = createScheduledSubject(
            CLASSROOM_2, TEACHER_ID, "3교시", TERM_START, "2099-03-31", "14:00:00", "14:30:00", 3
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of(
                "startAt", TERM_START,
                "endAt", TERM_END,
                "startTime", "13:00:00",
                "endTime", "13:30:00"
            ))
        .when()
            .patch("/{subjectId}/schedule", thirdPeriod)
        .then()
            .statusCode(200)
            .body("startTime", is("13:00:00"));

        assertThat(countActiveLessons(thirdPeriod)).isEqualTo(MONDAYS_IN_TERM);
    }

    @Test
    @DisplayName("PATCH /schedule: 시간만 바꿔 같은 교사의 앞 교시 종료 시각에 맞닿아도 수정된다")
    void updateSchedule_Success_WhenNewTimeTouchesTeacherLessonBoundary() {
        createSecondPeriodOfTeacher();
        long thirdPeriod = createScheduledSubject(
            CLASSROOM_2, TEACHER_ID, "3교시", TERM_START, TERM_END, "14:00:00", "14:30:00", 3
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of("startTime", "13:00:00", "endTime", "13:30:00"))
        .when()
            .patch("/{subjectId}/schedule", thirdPeriod)
        .then()
            .statusCode(200);

        Integer movedLessons = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM lessons
            WHERE subject_id = ? AND is_deleted = FALSE AND start_time = '13:00:00'
            """,
            Integer.class,
            thirdPeriod
        );
        assertThat(movedLessons).isEqualTo(MONDAYS_IN_TERM);
    }

    @Test
    @DisplayName("PATCH /schedule: 같은 교사의 앞 교시와 1분이라도 겹치면 409 Conflict")
    void updateSchedule_Conflict_WhenNewTimeOverlapsTeacherLessonByOneMinute() {
        createSecondPeriodOfTeacher();
        long thirdPeriod = createScheduledSubject(
            CLASSROOM_2, TEACHER_ID, "3교시", TERM_START, TERM_END, "14:00:00", "14:30:00", 3
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of("startTime", "12:59:00", "endTime", "13:30:00"))
        .when()
            .patch("/{subjectId}/schedule", thirdPeriod)
        .then()
            .statusCode(409)
            .body("code", is("BIZ-05-002"));
    }

    @Test
    @DisplayName("PATCH /schedule: 재생성 기간 중 한 날짜만 같은 교사 수업과 겹쳐도 409 Conflict")
    void updateSchedule_Conflict_WhenOnlyOneRecreatedDateOverlapsTeacherLesson() {
        long thirdPeriod = createScheduledSubject(
            CLASSROOM_2, TEACHER_ID, "3교시", TERM_START, "2099-03-31", "14:00:00", "14:30:00", 3
        );
        long otherSubject = createScheduledSubject(
            CLASSROOM_1, null, "하루 겹침", TERM_START, TERM_END, "13:10:00", "13:20:00", 2
        );
        jdbcTemplate.update(
            """
            INSERT INTO lessons (subject_id, teacher_id, date, start_time, end_time, period, status, is_deleted)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            otherSubject, TEACHER_ID, "2099-05-04", "13:10:00", "13:20:00", 2, "SCHEDULED", false
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of(
                "startAt", TERM_START,
                "endAt", TERM_END,
                "startTime", "13:00:00",
                "endTime", "13:30:00"
            ))
        .when()
            .patch("/{subjectId}/schedule", thirdPeriod)
        .then()
            .statusCode(409)
            .body("code", is("BIZ-05-002"));
    }

    @Test
    @DisplayName("PATCH /schedule: 시간만 바꿀 때 같은 과목의 기존 수업과는 겹침으로 보지 않는다")
    void updateSchedule_Success_WhenNewTimeOverlapsOnlyOwnLessons() {
        long subjectId = createScheduledSubject(
            CLASSROOM_2, TEACHER_ID, "3교시", TERM_START, TERM_END, "13:00:00", "13:30:00", 3
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of("startTime", "13:00:00", "endTime", "13:40:00"))
        .when()
            .patch("/{subjectId}/schedule", subjectId)
        .then()
            .statusCode(200)
            .body("endTime", is("13:40:00"));
    }

    @Test
    @DisplayName("PATCH /schedule: 다른 교사나 삭제된 수업은 같은 시간이어도 겹침으로 보지 않는다")
    void updateSchedule_Success_WhenSameTimeBelongsToOtherTeacherOrDeletedLesson() {
        long otherTeacherSubject = createScheduledSubject(
            CLASSROOM_1, NEW_TEACHER_ID, "다른 교사", TERM_START, TERM_END, "13:00:00", "13:30:00", 2
        );
        long deletedSubject = createScheduledSubject(
            CLASSROOM_1, TEACHER_ID, "삭제될 수업", TERM_START, TERM_END, "15:00:00", "15:30:00", 3
        );
        jdbcTemplate.update("UPDATE lessons SET is_deleted = TRUE WHERE subject_id = ?", deletedSubject);
        long target = createScheduledSubject(
            CLASSROOM_2, TEACHER_ID, "대상", TERM_START, TERM_END, "16:00:00", "16:30:00", 3
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of("startTime", "13:00:00", "endTime", "13:30:00"))
        .when()
            .patch("/{subjectId}/schedule", target)
        .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of("startTime", "15:00:00", "endTime", "15:30:00"))
        .when()
            .patch("/{subjectId}/schedule", target)
        .then()
            .statusCode(200);

        assertThat(countActiveLessons(otherTeacherSubject)).isEqualTo(MONDAYS_IN_TERM);
    }

    @Test
    @DisplayName("PATCH /teacher: 맞닿은 교시가 있는 교사를 배정해도 기간의 모든 수업을 만든다")
    void assignTeacher_CreatesEveryLesson_WhenTeacherHasAdjacentLesson() {
        createSecondPeriodOfTeacher();
        long thirdPeriod = createScheduledSubject(
            CLASSROOM_2, null, "3교시", TERM_START, TERM_END, "13:00:00", "13:30:00", 3
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of("teacherId", TEACHER_ID))
        .when()
            .patch("/{subjectId}/teacher", thirdPeriod)
        .then()
            .statusCode(200);

        assertThat(countActiveLessons(thirdPeriod)).isEqualTo(MONDAYS_IN_TERM);
    }

    @Test
    @DisplayName("POST: 맞닿은 교시가 있는 교사를 지정해 과목을 만들어도 모든 수업을 만든다")
    void createSubject_CreatesEveryLesson_WhenTeacherHasAdjacentLesson() {
        createSecondPeriodOfTeacher();

        long thirdPeriod = createScheduledSubject(
            CLASSROOM_2, TEACHER_ID, "3교시", TERM_START, TERM_END, "13:00:00", "13:30:00", 3
        );

        assertThat(countActiveLessons(thirdPeriod)).isEqualTo(MONDAYS_IN_TERM);
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
