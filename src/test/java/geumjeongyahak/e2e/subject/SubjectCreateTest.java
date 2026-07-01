package geumjeongyahak.e2e.subject;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("E2E: 과목 생성 테스트")
public class SubjectCreateTest extends SubjectBaseTest {

    private static final long CLASSROOM_ID = 1L;
    private static final long ADMIN_ID = 1L;
    private static final long TEACHER_ID = 2L;
    private static final long GUEST_ID = 4L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Map<String, Object> createRequest(String name, String startAt, String endAt, String dayOfWeek) {
        return Map.ofEntries(
            entry("classroomId", CLASSROOM_ID),
            entry("teacherId", TEACHER_ID),
            entry("name", name),
            entry("startAt", startAt),
            entry("endAt", endAt),
            entry("dayOfWeek", dayOfWeek),
            entry("startTime", "20:10:00"),
            entry("endTime", "20:50:00"),
            entry("period", 2),
            entry("description", "E2E 테스트 과목")
        );
    }

    @Test
    @DisplayName("관리자 권한으로 과목 생성 성공(201 Created)")
    void createSubject_Success_Admin() {
        Map<String, Object> request = createRequest(
            "국어",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", is("국어"))
            .body("period", is(2))
            .body("teacherAssignedAt", notNullValue())
            .log().all();
    }

    @Test
    @DisplayName("subject:write:* 권한으로 과목 생성 성공(201 Created)")
    void createSubject_Success_WithSubjectWritePermission() {
        Map<String, Object> request = createRequest(
            "쓰기 권한 과목",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(subjectWriteAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", is("쓰기 권한 과목"));
    }

    @Test
    @DisplayName("매니저 사용자를 교사로 지정해 과목 생성 성공(201 Created)")
    void createSubject_Success_WhenTeacherIsManager() {
        long managerId = 100L;
        jdbcTemplate.update("DELETE FROM user_permissions WHERE user_id = ?", managerId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ? OR primary_email = ?", managerId, "subject-manager@test.com");
        jdbcTemplate.update("""
            INSERT INTO users (id, name, primary_email, role)
            VALUES (?, ?, ?, ?)
            """, managerId, "과목 매니저", "subject-manager@test.com", "MANAGER");

        Map<String, Object> request = new HashMap<>(createRequest(
            "매니저 담당 과목",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        ));
        request.put("teacherId", managerId);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("teacherId", is((int) managerId))
            .body("name", is("매니저 담당 과목"));
    }

    @Test
    @DisplayName("관리자 사용자를 교사로 지정해 과목 생성 성공(201 Created)")
    void createSubject_Success_WhenTeacherIsAdmin() {
        Map<String, Object> request = new HashMap<>(createRequest(
            "관리자 담당 과목",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        ));
        request.put("teacherId", ADMIN_ID);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("teacherId", is((int) ADMIN_ID))
            .body("name", is("관리자 담당 과목"));
    }

    @Test
    @DisplayName("교사 미배정 상태로 과목 생성 시 수업을 자동 생성하지 않는다")
    void createSubject_Success_WithoutTeacherAndDoesNotCreateLessons() {
        Map<String, Object> request = new HashMap<>(createRequest(
            "교사 미배정 과목",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        ));
        request.remove("teacherId");

        Integer subjectId = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("teacherId", is((Object) null))
            .body("teacherAssignedAt", is((Object) null))
            .extract()
            .path("id");

        Integer lessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ?",
            Integer.class,
            subjectId
        );
        org.assertj.core.api.Assertions.assertThat(lessonCount).isZero();
    }

    @Test
    @DisplayName("담당 교사가 있는 과목 생성 시 수업과 하루 일정을 자동 생성한다")
    void createSubject_Success_WithTeacherCreatesLessonsAndDailySchedule() {
        LocalDate lessonDate = LocalDate.now().plusWeeks(2);
        while (lessonDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            lessonDate = lessonDate.plusDays(1);
        }

        Map<String, Object> request = createRequest(
            "하루 일정 자동 생성 과목",
            lessonDate.toString(),
            lessonDate.toString(),
            lessonDate.getDayOfWeek().name()
        );

        Integer subjectId = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract()
            .path("id");

        Integer lessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ?",
            Integer.class,
            subjectId
        );
        Integer dailyScheduleCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM daily_schedules WHERE classroom_id = ? AND lesson_date = ? AND is_deleted = false",
            Integer.class,
            CLASSROOM_ID,
            lessonDate
        );
        Integer teacherAttendanceCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM daily_teacher_attendances dta
                JOIN daily_schedules ds ON dta.daily_schedule_id = ds.id
                WHERE ds.classroom_id = ? AND ds.lesson_date = ? AND dta.is_deleted = false
                """,
            Integer.class,
            CLASSROOM_ID,
            lessonDate
        );

        assertThat(lessonCount).isEqualTo(1);
        assertThat(dailyScheduleCount).isEqualTo(1);
        assertThat(teacherAttendanceCount).isEqualTo(1);
    }

    @Test
    @DisplayName("담당 교사가 있는 과목 생성 시 교사의 기본 분반이 없으면 과목 분반으로 채운다")
    void createSubject_FillsTeacherDefaultClassroomWhenMissing() {
        jdbcTemplate.update("UPDATE users SET classroom_id = NULL WHERE id = ?", TEACHER_ID);
        try {
            Map<String, Object> request = new HashMap<>(createRequest(
                "기본 분반 자동 매핑 과목",
                "2026-03-03",
                "2026-06-30",
                "TUESDAY"
            ));

            given()
                .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
                .contentType("application/json")
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("teacherId", is((int) TEACHER_ID));

            Long userClassroomId = jdbcTemplate.queryForObject(
                "SELECT classroom_id FROM users WHERE id = ?",
                Long.class,
                TEACHER_ID
            );
            assertThat(userClassroomId).isEqualTo(CLASSROOM_ID);
        } finally {
            jdbcTemplate.update("UPDATE users SET classroom_id = ? WHERE id = ?", CLASSROOM_ID, TEACHER_ID);
        }
    }

    @Test
    @DisplayName("과목 생성 시 이미 지난 날짜의 수업은 자동 생성하지 않는다")
    void createSubject_DoesNotCreatePastLessons() {
        Map<String, Object> request = createRequest(
            "과거 수업 제외 과목",
            "2026-03-02",
            "2027-03-01",
            "MONDAY"
        );

        Integer subjectId = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract()
            .path("id");

        Integer pastLessonCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM lessons WHERE subject_id = ? AND date < ?",
            Integer.class,
            subjectId,
            LocalDate.now()
        );

        org.assertj.core.api.Assertions.assertThat(pastLessonCount).isZero();
    }

    @Test
    @DisplayName("일반 선생님 권한으로 과목 생성 실패(403 Forbidden)")
    void createSubject_Forbidden_Volunteer() {
        Map<String, Object> request = createRequest(
            "수학",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(403)
            .log().all();
    }

    @Test
    @DisplayName("인증 없이 과목 생성 실패(401 Unauthorized)")
    void createSubject_Unauthorized() {
        Map<String, Object> request = createRequest(
            "영어",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        );

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("검증 실패(기간 역전: startAt > endAt) 시 400 Bad Request")
    void createSubject_BadRequest_InvalidDateRange() {
        Map<String, Object> request = createRequest(
            "사회",
            "2026-06-30",
            "2026-03-02",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("검증 실패(운영 기간 365일 초과) 시 400 Bad Request")
    void createSubject_BadRequest_WhenOperationPeriodExceeds365Days() {
        Map<String, Object> request = createRequest(
            "장기 운영 과목",
            "2026-03-02",
            "2027-03-02",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(400)
            .body("code", is("VAL-05-001"))
            .body("detail", is("과목 운영 기간은 최대 365일까지 설정할 수 있습니다."))
            .log().all();
    }

    @Test
    @DisplayName("검증 실패(시간 역전: startTime >= endTime) 시 400 Bad Request")
    void createSubject_BadRequest_InvalidTimeRange() {
        Map<String, Object> request = Map.of(
            "classroomId", CLASSROOM_ID,
            "teacherId", TEACHER_ID,
            "name", "과학",
            "startAt", "2026-03-02",
            "endAt", "2026-06-30",
            "dayOfWeek", "MONDAY",
            "startTime", "20:00:00",
            "endTime", "19:20:00",
            "period", 1
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(400)
            .log().all();
    }

    @Test
    @DisplayName("기간이 겹치고 요일/교시가 같으면 생성 실패(409 Conflict)")
    void createSubject_Conflict_WhenOverlapAndSameSlot() {
        // 1) 먼저 하나 생성
        Map<String, Object> first = createRequest(
            "도덕",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(first)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue());

        // 2) 같은 반/요일/교시 + 기간 겹침 -> 409 기대
        Map<String, Object> conflict = createRequest(
            "미술",
            "2026-05-01",
            "2026-07-01",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(conflict)
            .when()
            .post()
            .then()
            .statusCode(409)
            .log().all();
    }

    @Test
    @DisplayName("비활성 과목과 기간/요일/교시가 겹쳐도 생성 성공(201 Created)")
    void createSubject_Success_WhenOnlyInactiveSubjectOverlapsSameSlot() {
        Map<String, Object> inactive = createRequest(
            "비활성 중복 기준 과목",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        );

        Integer inactiveSubjectId = given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(inactive)
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract()
            .path("id");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
        .when()
            .delete("/{subjectId}", inactiveSubjectId)
        .then()
            .statusCode(204);

        Map<String, Object> request = createRequest(
            "활성 재생성 과목",
            "2026-05-01",
            "2026-07-01",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", is("활성 재생성 과목"))
            .log().all();
    }

    @Test
    @DisplayName("교사 배정 불가 사용자를 교사로 지정하면 과목 생성 실패(400 Bad Request)")
    void createSubject_BadRequest_WhenTeacherIsNotAssignable() {
        Map<String, Object> request = Map.ofEntries(
            entry("classroomId", CLASSROOM_ID),
            entry("teacherId", GUEST_ID),
            entry("name", "잘못된 교사 배정"),
            entry("startAt", "2026-03-02"),
            entry("endAt", "2026-06-30"),
            entry("dayOfWeek", "MONDAY"),
            entry("startTime", "20:10:00"),
            entry("endTime", "20:50:00"),
            entry("period", 2),
            entry("description", "E2E 테스트 과목")
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(400)
            .body("code", is("VAL002"));
    }

    @Test
    @DisplayName("요일/교시가 같아도 기간이 겹치지 않으면 생성 성공(201 Created)")
    void createSubject_Success_WhenNotOverlapEvenSameSlot() {
        // 먼저 하나 생성
        Map<String, Object> first = createRequest(
            "음악",
            "2026-03-02",
            "2026-06-30",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(first)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue());

        // 같은 요일/교시지만 기간이 겹치지 않음 -> 성공
        Map<String, Object> second = createRequest(
            "체육",
            "2026-07-01",
            "2026-09-30",
            "MONDAY"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(second)
            .when()
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", is("체육"))
            .log().all();
    }
}
