package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 교사 수업 시간 겹침 판정 (#240). 끝과 시작이 맞닿은 수업은 겹치지 않는다. */
@DisplayName("E2E: 과목 일정·교사 배정의 교사 수업 겹침 판정")
class SubjectTeacherOverlapTest extends SubjectBaseTest {

    private static final long CLASSROOM_1 = 1L;
    private static final long TEACHER_ID = 2L;
    private static final long NEW_TEACHER_ID = 3L;
    private static final long CLASSROOM_2 = 2L;
    private static final String TERM_START = "2099-03-02";
    private static final String TERM_END = "2099-06-30";
    private static final int MONDAYS_IN_TERM = 18;

    /**
     * 교사 TEACHER_ID의 벚꽃반 월요일 2교시 12:20–13:00 수업을 학기 전체에 만든다.
     * 교사 배정은 같은 분반·요일에만 허용되므로(#199) 배정 경로 테스트는 같은 분반을 쓴다.
     * 일정 변경 경로는 분반 중복 검사를 피하려고 다른 분반(CLASSROOM_2)을 쓴다.
     */
    private void createSecondPeriodOfTeacher() {
        long secondPeriod = createMondaySubject(
            CLASSROOM_1, TEACHER_ID, "2교시", TERM_START, TERM_END, "12:20:00", "13:00:00", 2
        );
        assertThat(countActiveLessons(secondPeriod)).isEqualTo(MONDAYS_IN_TERM);
    }

    @Test
    @DisplayName("PATCH /schedule: 기간을 바꾸며 같은 교사의 앞 교시 종료 시각에 맞닿게 옮기면 수업을 재생성한다")
    void updateSchedule_Success_WhenRecreatedLessonsTouchTeacherLessonBoundary() {
        createSecondPeriodOfTeacher();
        long thirdPeriod = createMondaySubject(
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
        long thirdPeriod = createMondaySubject(
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
        long thirdPeriod = createMondaySubject(
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
        long thirdPeriod = createMondaySubject(
            CLASSROOM_2, TEACHER_ID, "3교시", TERM_START, "2099-03-31", "14:00:00", "14:30:00", 3
        );
        long otherSubject = createMondaySubject(
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
        long subjectId = createMondaySubject(
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
    @DisplayName("PATCH /schedule: 시간만 바꿀 때 같은 날 같은 과목 수업끼리 겹치게 되면 409 Conflict")
    void updateSchedule_Conflict_WhenOwnLessonsOnSameDateWouldOverlap() {
        long subjectId = createMondaySubject(
            CLASSROOM_2, TEACHER_ID, "3교시", TERM_START, TERM_END, "10:00:00", "11:00:00", 3
        );
        jdbcTemplate.update(
            """
            INSERT INTO lessons (subject_id, teacher_id, date, start_time, end_time, period, status, is_deleted)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            subjectId, TEACHER_ID, java.time.LocalDate.parse(TERM_START), java.time.LocalTime.of(12, 0),
            java.time.LocalTime.of(13, 0), 4, "SCHEDULED", false
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(Map.of("startTime", "10:30:00", "endTime", "11:30:00"))
        .when()
            .patch("/{subjectId}/schedule", subjectId)
        .then()
            .statusCode(409)
            .body("code", is("BIZ-05-002"));
    }

    @Test
    @DisplayName("PATCH /schedule: 다른 교사나 삭제된 수업은 같은 시간이어도 겹침으로 보지 않는다")
    void updateSchedule_Success_WhenSameTimeBelongsToOtherTeacherOrDeletedLesson() {
        long otherTeacherSubject = createMondaySubject(
            CLASSROOM_1, NEW_TEACHER_ID, "다른 교사", TERM_START, TERM_END, "13:00:00", "13:30:00", 2
        );
        long deletedSubject = createMondaySubject(
            CLASSROOM_1, TEACHER_ID, "삭제될 수업", TERM_START, TERM_END, "15:00:00", "15:30:00", 3
        );
        jdbcTemplate.update("UPDATE lessons SET is_deleted = TRUE WHERE subject_id = ?", deletedSubject);
        long target = createMondaySubject(
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
        long thirdPeriod = createMondaySubject(
            CLASSROOM_1, null, "3교시", TERM_START, TERM_END, "13:00:00", "13:30:00", 3
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

        long thirdPeriod = createMondaySubject(
            CLASSROOM_1, TEACHER_ID, "3교시", TERM_START, TERM_END, "13:00:00", "13:30:00", 3
        );

        assertThat(countActiveLessons(thirdPeriod)).isEqualTo(MONDAYS_IN_TERM);
    }
}
