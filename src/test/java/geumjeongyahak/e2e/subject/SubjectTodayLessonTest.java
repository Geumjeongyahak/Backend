package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.common.config.AppConfig;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 과목 수정은 오늘 수업이 시작 전이면 오늘부터, 시작했으면 내일부터 수업에 반영된다 (#241).
 * 테스트와 서버가 같은 시각을 보도록 오늘 정오로 고정한 Clock을 쓴다.
 * 06:00 수업은 「이미 시작한 수업」, 18:00 수업은 「아직 시작 전인 수업」이다.
 */
@DisplayName("E2E: 과목 수정 시 당일 수업 보존 (#241)")
public class SubjectTodayLessonTest extends SubjectBaseTest {

    private static final long CLASSROOM_1 = DEFAULT_CLASSROOM_ID;
    private static final long TEACHER_ID = DEFAULT_TEACHER_ID;
    private static final long NEW_TEACHER_ID = 3L;
    private static final int SEED_CLASSROOM_1_STUDENTS = 2;
    private static final String STARTED = "06:00:00";
    private static final String NOT_STARTED = "18:00:00";

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            LocalDate today = LocalDate.now(AppConfig.ZONE_ID);
            return Clock.fixed(today.atTime(LocalTime.NOON).atZone(AppConfig.ZONE_ID).toInstant(), AppConfig.ZONE_ID);
        }
    }

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("수업 시작 뒤 PATCH /schedule: 기간을 미래로 옮겨도 당일 수업과 DailySchedule·출석은 남는다")
    void updateSchedule_KeepsTodayLesson_WhenPeriodMovesToFuture() {
        LocalDate today = LocalDate.now(clock);
        long subjectId = createTodaySubject(today, STARTED);
        long todayLessonId = activeLessonId(subjectId, today);
        long dailyScheduleId = activeDailyScheduleId(today);

        patch("/{subjectId}/schedule", subjectId,
            Map.of("startAt", today.plusDays(1).toString(), "endAt", today.plusDays(30).toString()));

        assertThat(activeLessonId(subjectId, today)).isEqualTo(todayLessonId);
        assertTodayRecordsKept(dailyScheduleId, today);
        assertThat(activeLessonDates(subjectId)).startsWith(today, today.plusDays(7));
    }

    @Test
    @DisplayName("수업 시작 뒤 PATCH /schedule: 종료일만 연장하면 당일 수업을 다시 만들지 않는다")
    void updateSchedule_KeepsTodayLesson_WhenEndAtExtended() {
        LocalDate today = LocalDate.now(clock);
        long subjectId = createTodaySubject(today, STARTED);
        long todayLessonId = activeLessonId(subjectId, today);
        long dailyScheduleId = activeDailyScheduleId(today);

        patch("/{subjectId}/schedule", subjectId, Map.of("endAt", today.plusDays(60).toString()));

        assertThat(activeLessonId(subjectId, today)).isEqualTo(todayLessonId);
        assertTodayRecordsKept(dailyScheduleId, today);
    }

    @Test
    @DisplayName("수업 시작 뒤 PATCH /teacher: 담당 교사를 해제해도 당일 수업은 남고 내일부터 지운다")
    void unassignTeacher_KeepsTodayLesson() {
        LocalDate today = LocalDate.now(clock);
        long subjectId = createTodaySubject(today, STARTED);
        long todayLessonId = activeLessonId(subjectId, today);
        long dailyScheduleId = activeDailyScheduleId(today);

        patch("/{subjectId}/teacher", subjectId, teacherRequest(null));

        assertThat(activeLessonId(subjectId, today)).isEqualTo(todayLessonId);
        assertThat(lessonTeacherId(todayLessonId)).isEqualTo(TEACHER_ID);
        assertTodayRecordsKept(dailyScheduleId, today);
        assertThat(activeLessonDates(subjectId)).containsExactly(today);
    }

    @Test
    @DisplayName("수업 시작 뒤 DELETE: 과목을 삭제해도 당일 수업은 남고 내일부터 지운다")
    void deleteSubject_KeepsTodayLesson() {
        LocalDate today = LocalDate.now(clock);
        long subjectId = createTodaySubject(today, STARTED);
        long todayLessonId = activeLessonId(subjectId, today);
        long dailyScheduleId = activeDailyScheduleId(today);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .delete("/{subjectId}", subjectId)
            .then()
            .statusCode(204);

        assertThat(activeLessonId(subjectId, today)).isEqualTo(todayLessonId);
        assertTodayRecordsKept(dailyScheduleId, today);
        assertThat(activeLessonDates(subjectId)).containsExactly(today);
    }

    @Test
    @DisplayName("수업 시작 뒤 PATCH /teacher: 담당 교사를 바꾸면 당일 수업은 옛 교사로 두고 내일부터 바꾼다")
    void replaceTeacher_KeepsTodayLessonTeacher() {
        LocalDate today = LocalDate.now(clock);
        long subjectId = createTodaySubject(today, STARTED);
        long todayLessonId = activeLessonId(subjectId, today);
        long dailyScheduleId = activeDailyScheduleId(today);

        patch("/{subjectId}/teacher", subjectId, teacherRequest(NEW_TEACHER_ID));

        assertThat(lessonTeacherId(todayLessonId)).isEqualTo(TEACHER_ID);
        assertThat(dailyScheduleTeacherId(dailyScheduleId)).isEqualTo(TEACHER_ID);
        assertTodayRecordsKept(dailyScheduleId, today);
        assertThat(lessonTeacherId(activeLessonId(subjectId, today.plusDays(7)))).isEqualTo(NEW_TEACHER_ID);
    }

    @Test
    @DisplayName("수업 시작 뒤 PATCH /teacher: 해제한 날 다시 배정하면 당일 수업은 하나로 두고 내일부터 새로 만든다")
    void reassignTeacherSameDay_CreatesLessonsFromTomorrow() {
        LocalDate today = LocalDate.now(clock);
        long subjectId = createTodaySubject(today, STARTED);
        long todayLessonId = activeLessonId(subjectId, today);

        patch("/{subjectId}/teacher", subjectId, teacherRequest(null));
        patch("/{subjectId}/teacher", subjectId, teacherRequest(NEW_TEACHER_ID));

        assertThat(activeLessonId(subjectId, today)).isEqualTo(todayLessonId);
        assertThat(lessonTeacherId(todayLessonId)).isEqualTo(TEACHER_ID);
        assertThat(lessonTeacherId(activeLessonId(subjectId, today.plusDays(7)))).isEqualTo(NEW_TEACHER_ID);
    }

    @Test
    @DisplayName("수업 시작 전 PATCH /schedule: 기간을 미래로 옮기면 오늘 수업도 지운다")
    void updateSchedule_DeletesTodayLesson_WhenNotStarted() {
        LocalDate today = LocalDate.now(clock);
        long subjectId = createTodaySubject(today, NOT_STARTED);

        patch("/{subjectId}/schedule", subjectId,
            Map.of("startAt", today.plusDays(1).toString(), "endAt", today.plusDays(30).toString()));

        assertThat(activeLessonDates(subjectId)).first().isEqualTo(today.plusDays(7));
    }

    @Test
    @DisplayName("수업 시작 전 PATCH /teacher: 담당 교사를 바꾸면 오늘 수업 교사도 바꾼다")
    void replaceTeacher_ChangesTodayLessonTeacher_WhenNotStarted() {
        LocalDate today = LocalDate.now(clock);
        long subjectId = createTodaySubject(today, NOT_STARTED);
        long todayLessonId = activeLessonId(subjectId, today);

        patch("/{subjectId}/teacher", subjectId, teacherRequest(NEW_TEACHER_ID));

        assertThat(lessonTeacherId(todayLessonId)).isEqualTo(NEW_TEACHER_ID);
    }

    @Test
    @DisplayName("수업 시작 전 PATCH /schedule: 요일을 오늘 요일로 바꾸면 오늘 수업을 만든다")
    void updateSchedule_CreatesTodayLesson_WhenDayOfWeekBecomesTodayBeforeStart() {
        LocalDate today = LocalDate.now(clock);
        long subjectId = createSubject(today, today.plusDays(1).getDayOfWeek().name(), NOT_STARTED);
        assertThat(activeLessonDates(subjectId)).doesNotContain(today);

        patch("/{subjectId}/schedule", subjectId, Map.of("dayOfWeek", today.getDayOfWeek().name()));

        activeLessonId(subjectId, today);
    }

    @Test
    @DisplayName("수업 시작 뒤 PATCH /schedule: 시간만 바꾸면 오늘 수업 시간은 그대로 두고 다음 수업부터 바꾼다")
    void updateScheduleTime_KeepsTodayLessonTime_WhenStarted() {
        LocalDate today = LocalDate.now(clock);
        long subjectId = createTodaySubject(today, STARTED);
        long todayLessonId = activeLessonId(subjectId, today);

        patch("/{subjectId}/schedule", subjectId, Map.of("startTime", "07:00:00", "endTime", "07:40:00"));

        assertThat(lessonStartTime(todayLessonId)).isEqualTo(LocalTime.of(6, 0));
        assertThat(lessonStartTime(activeLessonId(subjectId, today.plusDays(7)))).isEqualTo(LocalTime.of(7, 0));
    }

    @Test
    @DisplayName("수업 시작 뒤 시간을 늦춘 같은 날 교사를 바꿔도 이미 시작한 오늘 수업은 그대로다")
    void replaceTeacher_KeepsStartedTodayLesson_AfterTimeMovedLater() {
        LocalDate today = LocalDate.now(clock);
        long subjectId = createTodaySubject(today, STARTED);
        long todayLessonId = activeLessonId(subjectId, today);

        // 과목 시각은 18:00(시작 전)이 되지만 오늘 수업은 06:00 그대로 남는다
        patch("/{subjectId}/schedule", subjectId, Map.of("startTime", NOT_STARTED, "endTime", "18:40:00"));
        patch("/{subjectId}/teacher", subjectId, teacherRequest(NEW_TEACHER_ID));

        assertThat(lessonTeacherId(todayLessonId)).isEqualTo(TEACHER_ID);
        assertThat(lessonStartTime(todayLessonId)).isEqualTo(LocalTime.of(6, 0));
    }

    /** 오늘 시작, 요일 = 오늘 요일인 과목. 만들 때 오늘 수업과 DailySchedule·출석이 생긴다. */
    private long createTodaySubject(LocalDate today, String startTime) {
        return createSubject(today, today.getDayOfWeek().name(), startTime);
    }

    /** 오늘부터 30일, 수업 40분인 과목. */
    private long createSubject(LocalDate today, String dayOfWeek, String startTime) {
        Map<String, Object> request = new HashMap<>(createRequest(CLASSROOM_1, "오늘 과목", dayOfWeek, 1));
        request.put("startAt", today.toString());
        request.put("endAt", today.plusDays(30).toString());
        request.put("startTime", startTime);
        request.put("endTime", LocalTime.parse(startTime).plusMinutes(40).toString() + ":00");

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

    private Map<String, Object> teacherRequest(Long teacherId) {
        Map<String, Object> request = new HashMap<>();
        request.put("teacherId", teacherId);
        return request;
    }

    private void patch(String path, long subjectId, Map<String, Object> request) {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .patch(path, subjectId)
            .then()
            .statusCode(200);
    }

    /** 그 날짜의 활성 수업 id. 2건 이상이거나 없으면 queryForObject가 실패한다. */
    private long activeLessonId(long subjectId, LocalDate date) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM lessons WHERE subject_id = ? AND date = ? AND is_deleted = FALSE",
            Long.class,
            subjectId,
            date
        );
    }

    private List<LocalDate> activeLessonDates(long subjectId) {
        return jdbcTemplate.query(
            "SELECT date FROM lessons WHERE subject_id = ? AND is_deleted = FALSE ORDER BY date",
            (rs, rowNum) -> rs.getDate("date").toLocalDate(),
            subjectId
        );
    }

    private long lessonTeacherId(long lessonId) {
        return jdbcTemplate.queryForObject("SELECT teacher_id FROM lessons WHERE id = ?", Long.class, lessonId);
    }

    private LocalTime lessonStartTime(long lessonId) {
        return jdbcTemplate.queryForObject("SELECT start_time FROM lessons WHERE id = ?", LocalTime.class, lessonId);
    }

    private long dailyScheduleTeacherId(long dailyScheduleId) {
        return jdbcTemplate.queryForObject("SELECT teacher_id FROM daily_schedules WHERE id = ?", Long.class, dailyScheduleId);
    }

    private long activeDailyScheduleId(LocalDate date) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM daily_schedules WHERE classroom_id = ? AND lesson_date = ? AND is_deleted = FALSE",
            Long.class,
            CLASSROOM_1,
            date
        );
    }

    private void assertTodayRecordsKept(long dailyScheduleId, LocalDate today) {
        assertThat(activeDailyScheduleId(today)).isEqualTo(dailyScheduleId);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM daily_teacher_attendances WHERE daily_schedule_id = ? AND is_deleted = FALSE",
            Integer.class,
            dailyScheduleId
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM daily_student_attendances WHERE daily_schedule_id = ? AND is_deleted = FALSE",
            Integer.class,
            dailyScheduleId
        )).isEqualTo(SEED_CLASSROOM_1_STUDENTS);
    }
}
