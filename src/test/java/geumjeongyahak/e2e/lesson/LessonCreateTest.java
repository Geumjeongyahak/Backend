package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import geumjeongyahak.domain.auth.enums.RoleType;

@DisplayName("E2E: Lesson 생성 테스트")
public class LessonCreateTest extends LessonBaseTest {
    private static final long GUEST_ID = 4L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("관리자 권한으로 수업 생성 성공(201)")
    void createLesson_success_admin() {
        Long subjectId = createTrackedSubjectAndGetId("국어");
        createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-20", "19:20:00", "20:00:00", 1);
    }

    @Test
    @DisplayName("lesson:write:* 권한으로 수업 생성 성공(201)")
    void createLesson_success_lessonWritePermission() {
        Long subjectId = createTrackedSubjectAndGetId("매니저 생성");
        Map<String, Object> request = createLessonRequest(
            subjectId, TEACHER2_ID, "2026-02-21", "19:20:00", "20:00:00", 1
        );

        String lessonWriteToken = createAccessTokenWithPermission("lesson-write", RoleType.VOLUNTEER, "lesson:write:*");
        createLessonAndGetId(request, lessonWriteToken);
    }

    @Test
    @DisplayName("수업 생성 시 기본 분반이 없는 교사를 지정하면 과목 분반으로 채운다")
    void createLesson_fillsTeacherDefaultClassroomWhenMissing() {
        jdbcTemplate.update("UPDATE users SET classroom_id = NULL WHERE id = ?", TEACHER2_ID);
        try {
            Long subjectId = createTrackedSubjectAndGetId("수업 기본 분반");

            createTrackedLessonAndGetId(subjectId, TEACHER2_ID, "2026-02-27", "19:20:00", "20:00:00", 1);

            Long userClassroomId = jdbcTemplate.queryForObject(
                "SELECT classroom_id FROM users WHERE id = ?",
                Long.class,
                TEACHER2_ID
            );
            assertThat(userClassroomId).isEqualTo(CLASSROOM_ID);
        } finally {
            jdbcTemplate.update("UPDATE users SET classroom_id = NULL WHERE id = ?", TEACHER2_ID);
        }
    }

    @Test
    @DisplayName("매니저 역할만으로 수업 생성 실패(403)")
    void createLesson_forbidden_managerWithoutLessonWritePermission() {
        Long subjectId = createTrackedSubjectAndGetId("매니저 생성 제한");
        Map<String, Object> request = createLessonRequest(
            subjectId, TEACHER2_ID, "2026-02-21", "19:20:00", "20:00:00", 1
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(managerAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("일반 봉사자 권한으로 수업 생성 실패(403)")
    void createLesson_forbidden_volunteer() {
        Long subjectId = createTrackedSubjectAndGetId("수학");

        Map<String, Object> request = createLessonRequest(
            subjectId, TEACHER_ID, "2026-02-22", "19:20:00", "20:00:00", 1
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("인증 없이 수업 생성 실패(401)")
    void createLesson_unauthorized() {
        Long subjectId = createTrackedSubjectAndGetId("영어");

        Map<String, Object> request = createLessonRequest(
            subjectId, TEACHER_ID, "2026-02-23", "19:20:00", "20:00:00", 1
        );

        given()
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("시간 유효성 실패(startTime >= endTime)면 400")
    void createLesson_badRequest_invalidTime() {
        Long subjectId = createTrackedSubjectAndGetId("사회");

        Map<String, Object> request = createLessonRequest(
            subjectId, TEACHER_ID, "2026-02-24", "20:00:00", "19:20:00", 1
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("같은 teacher + 같은 date에 시간 겹치면 409")
    void createLesson_conflict_overlappingTime() {
        Long subjectId = createTrackedSubjectAndGetId("과학");
        String date = LocalDate.of(2026, 2, 25).toString();

        // 수업 생성
        createTrackedLessonAndGetId(subjectId, TEACHER_ID, date, "19:00:00", "21:00:00", 1);

        // 겹치는 수업 생성 -> 409
        Map<String, Object> conflict = createLessonRequest(
            subjectId, TEACHER_ID, date, "19:30:00", "20:00:00", 2
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(conflict)
            .when()
            .post()
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("봉사자가 아닌 사용자를 교사로 지정하면 수업 생성 실패(400)")
    void createLesson_badRequest_whenTeacherIsNotVolunteer() {
        Long subjectId = createTrackedSubjectAndGetId("비정상 교사 배정");

        Map<String, Object> request = createLessonRequest(
            subjectId, GUEST_ID, "2026-02-26", "19:20:00", "20:00:00", 1
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(request)
            .when()
            .post()
            .then()
            .statusCode(400);
    }
}
