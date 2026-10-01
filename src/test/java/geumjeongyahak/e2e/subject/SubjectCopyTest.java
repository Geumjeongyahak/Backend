package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import io.restassured.response.ValidatableResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 시간표 기간 복사 (#242). 보낸 과목만 새 기간으로 복사하고, 하나라도 실패하면 아무것도 저장하지 않는다. */
@DisplayName("E2E: POST /subjects/copy 시간표 기간 복사")
class SubjectCopyTest extends SubjectBaseTest {

    private static final long CLASSROOM_1 = 1L;
    private static final long CLASSROOM_2 = 2L;
    private static final long TEACHER_ID = 2L;
    private static final String SOURCE_START = "2099-03-02";
    private static final String SOURCE_END = "2099-03-31";
    private static final String TARGET_START = "2099-04-01";
    private static final String TARGET_END = "2099-04-30";
    private static final int MONDAYS_IN_TARGET = 4;
    private static final int MONDAYS_IN_SOURCE = 5;

    private ValidatableResponse copy(String token, Map<String, Object> body) {
        return given()
            .header(AUTH_HEADER, getAuthHeader(token))
            .contentType("application/json")
            .body(body)
        .when()
            .post("/copy")
        .then();
    }

    private Map<String, Object> copyRequest(List<Long> subjectIds) {
        return Map.of("subjectIds", subjectIds, "startAt", TARGET_START, "endAt", TARGET_END);
    }

    private int countSubjectsStartingAt(String startAt) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM subjects WHERE start_at = ? AND is_active = TRUE",
            Integer.class,
            java.time.LocalDate.parse(startAt)
        );
    }

    private int countActiveLessonsBetween(String from, String to) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE is_deleted = FALSE AND date BETWEEN ? AND ?",
            Integer.class,
            java.time.LocalDate.parse(from),
            java.time.LocalDate.parse(to)
        );
    }

    @Test
    @DisplayName("보낸 과목을 새 기간으로 복사하고 교사가 있는 과목은 새 기간 수업을 만든다. 원본은 그대로다")
    void copy_CreatesSubjectsAndLessons_KeepsSource() {
        long first = createMondaySubject(CLASSROOM_1, TEACHER_ID, "1교시", SOURCE_START, SOURCE_END, "19:20:00", "20:00:00", 1);
        long second = createMondaySubject(CLASSROOM_1, TEACHER_ID, "2교시", SOURCE_START, SOURCE_END, "20:10:00", "20:50:00", 2);
        long noTeacher = createMondaySubject(CLASSROOM_2, null, "미배정", SOURCE_START, SOURCE_END, "19:20:00", "20:00:00", 1);

        copy(adminAccessToken, copyRequest(List.of(first, second, noTeacher)))
            .statusCode(201)
            .body("copiedCount", is(3))
            .body("subjects", hasSize(3))
            .body("subjects.startAt", containsInAnyOrder(TARGET_START, TARGET_START, TARGET_START))
            .body("subjects.name", containsInAnyOrder("1교시", "2교시", "미배정"));

        assertThat(countSubjectsStartingAt(TARGET_START)).isEqualTo(3);
        assertThat(countActiveLessonsBetween(TARGET_START, TARGET_END)).isEqualTo(2 * MONDAYS_IN_TARGET);
        assertThat(countActiveLessons(first)).isEqualTo(MONDAYS_IN_SOURCE);
        assertThat(countSubjectsStartingAt(SOURCE_START)).isEqualTo(3);
    }

    @Test
    @DisplayName("일부 과목만 보내면 그 과목만 복사한다")
    void copy_OnlyRequestedSubjects() {
        createMondaySubject(CLASSROOM_1, TEACHER_ID, "1교시", SOURCE_START, SOURCE_END, "19:20:00", "20:00:00", 1);
        long noTeacher = createMondaySubject(CLASSROOM_2, null, "미배정", SOURCE_START, SOURCE_END, "19:20:00", "20:00:00", 1);

        copy(adminAccessToken, copyRequest(List.of(noTeacher)))
            .statusCode(201)
            .body("copiedCount", is(1));

        assertThat(countSubjectsStartingAt(TARGET_START)).isEqualTo(1);
    }

    @Test
    @DisplayName("하나라도 실패하면 실패한 과목 전부를 사유와 함께 409로 돌려주고 아무것도 저장하지 않는다")
    void copy_CollectsAllFailures_SavesNothing() {
        long first = createMondaySubject(CLASSROOM_1, TEACHER_ID, "1교시", SOURCE_START, SOURCE_END, "19:20:00", "20:00:00", 1);
        long second = createMondaySubject(CLASSROOM_1, TEACHER_ID, "2교시", SOURCE_START, SOURCE_END, "20:10:00", "20:50:00", 2);
        long noTeacher = createMondaySubject(CLASSROOM_2, null, "미배정", SOURCE_START, SOURCE_END, "19:20:00", "20:00:00", 1);
        // 새 기간 1반 월 1교시 자리에 이미 과목이 있다
        createMondaySubject(CLASSROOM_1, null, "선점", TARGET_START, TARGET_END, "19:20:00", "20:00:00", 1);
        // 같은 교사가 새 기간에 다른 분반 하루치 일정을 맡고 있다 (#199)
        createMondaySubject(CLASSROOM_2, TEACHER_ID, "다른 분반", TARGET_START, TARGET_END, "20:10:00", "20:50:00", 2);
        int subjectsBefore = countSubjectsStartingAt(TARGET_START);
        int lessonsBefore = countActiveLessonsBetween(TARGET_START, TARGET_END);

        copy(adminAccessToken, copyRequest(List.of(first, second, noTeacher)))
            .statusCode(409)
            .body("code", is("BIZ-05-003"))
            .body("failures", hasSize(2))
            .body("failures.sourceSubjectId", containsInAnyOrder((int) first, (int) second))
            .body("failures.classroomId", containsInAnyOrder((int) CLASSROOM_1, (int) CLASSROOM_1))
            .body("failures.dayOfWeek", containsInAnyOrder("MONDAY", "MONDAY"))
            .body("failures.period", containsInAnyOrder(1, 2));

        assertThat(countSubjectsStartingAt(TARGET_START)).isEqualTo(subjectsBefore);
        assertThat(countActiveLessonsBetween(TARGET_START, TARGET_END)).isEqualTo(lessonsBefore);
    }

    @Test
    @DisplayName("같은 복사를 두 번 보내면 두 번째는 모든 과목이 실패해 409")
    void copy_Twice_SecondConflicts() {
        long first = createMondaySubject(CLASSROOM_1, TEACHER_ID, "1교시", SOURCE_START, SOURCE_END, "19:20:00", "20:00:00", 1);

        copy(adminAccessToken, copyRequest(List.of(first))).statusCode(201);
        copy(adminAccessToken, copyRequest(List.of(first)))
            .statusCode(409)
            .body("failures", hasSize(1));
    }

    @Test
    @DisplayName("보낸 과목끼리 같은 칸·시간으로 겹치면 둘 다 실패로 돌려준다")
    void copy_RequestedSubjectsCollideWithEachOther() {
        long spring = createMondaySubject(CLASSROOM_1, null, "상반기", "2099-02-02", "2099-02-23", "19:20:00", "20:00:00", 1);
        long march = createMondaySubject(CLASSROOM_1, null, "3월", SOURCE_START, SOURCE_END, "19:20:00", "20:00:00", 1);

        copy(adminAccessToken, copyRequest(List.of(spring, march)))
            .statusCode(409)
            .body("failures", hasSize(2))
            .body("failures.sourceSubjectId", containsInAnyOrder((int) spring, (int) march));
        assertThat(countSubjectsStartingAt(TARGET_START)).isZero();
    }

    @Test
    @DisplayName("같은 교사의 맞닿은 교시도 그대로 복사된다 (#240)")
    void copy_AdjacentPeriodsOfSameTeacher() {
        long second = createMondaySubject(CLASSROOM_1, TEACHER_ID, "2교시", SOURCE_START, SOURCE_END, "12:20:00", "13:00:00", 2);
        long third = createMondaySubject(CLASSROOM_1, TEACHER_ID, "3교시", SOURCE_START, SOURCE_END, "13:00:00", "13:30:00", 3);

        copy(adminAccessToken, copyRequest(List.of(second, third))).statusCode(201);

        assertThat(countActiveLessonsBetween(TARGET_START, TARGET_END)).isEqualTo(2 * MONDAYS_IN_TARGET);
    }

    @Test
    @DisplayName("subjectIds가 비었거나 중복이거나 없는 과목이면 400")
    void copy_InvalidSubjectIds_BadRequest() {
        long first = createMondaySubject(CLASSROOM_1, null, "1교시", SOURCE_START, SOURCE_END, "19:20:00", "20:00:00", 1);

        copy(adminAccessToken, copyRequest(List.of())).statusCode(400);
        copy(adminAccessToken, copyRequest(List.of(first, first))).statusCode(400);
        copy(adminAccessToken, copyRequest(List.of(first, 999_999L))).statusCode(400);
        assertThat(countSubjectsStartingAt(TARGET_START)).isZero();
    }

    @Test
    @DisplayName("비활성(삭제된) 과목은 복사할 수 없어 400")
    void copy_InactiveSubject_BadRequest() {
        long first = createMondaySubject(CLASSROOM_1, null, "1교시", SOURCE_START, SOURCE_END, "19:20:00", "20:00:00", 1);
        jdbcTemplate.update("UPDATE subjects SET is_active = FALSE WHERE id = ?", first);

        copy(adminAccessToken, copyRequest(List.of(first))).statusCode(400);
    }

    @Test
    @DisplayName("새 기간이 거꾸로이거나 365일을 넘으면 400")
    void copy_InvalidPeriod_BadRequest() {
        long first = createMondaySubject(CLASSROOM_1, null, "1교시", SOURCE_START, SOURCE_END, "19:20:00", "20:00:00", 1);

        copy(adminAccessToken, Map.of("subjectIds", List.of(first), "startAt", TARGET_END, "endAt", TARGET_START))
            .statusCode(400);
        copy(adminAccessToken, Map.of("subjectIds", List.of(first), "startAt", "2099-04-01", "endAt", "2100-04-30"))
            .statusCode(400);
    }

    @Test
    @DisplayName("과목 쓰기 권한(subject:write:*)은 복사할 수 있고 봉사자는 403")
    void copy_Permission() {
        long first = createMondaySubject(CLASSROOM_1, null, "1교시", SOURCE_START, SOURCE_END, "19:20:00", "20:00:00", 1);

        copy(volunteerAccessToken, copyRequest(List.of(first))).statusCode(403);
        copy(subjectWriteAccessToken, copyRequest(List.of(first))).statusCode(201);
    }
}
