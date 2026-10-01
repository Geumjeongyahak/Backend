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

/** PATCH /subjects/{id}/schedule — 일정 변경과 수업 갱신·재생성. */
@DisplayName("E2E: 과목 일정 PATCH /schedule 테스트")
public class SubjectScheduleUpdateTest extends SubjectBaseTest {

    private static final long CLASSROOM_1 = DEFAULT_CLASSROOM_ID;
    private static final long TEACHER_ID = DEFAULT_TEACHER_ID;
    private static final long NEW_TEACHER_ID = 3L;

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
}
