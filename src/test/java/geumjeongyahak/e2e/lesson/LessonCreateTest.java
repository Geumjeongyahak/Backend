package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: Lesson 생성 테스트")
public class LessonCreateTest extends LessonBaseTest {

    private final List<Long> createdLessonIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long lessonId : createdLessonIds) {
            deleteLesson(lessonId, adminAccessToken);
        }
        createdLessonIds.clear();
    }

    @Test
    @DisplayName("관리자 권한으로 수업 생성 성공(201)")
    void createLesson_success_admin() {
        Long subjectId = createSubjectAndGetId("국어");

        Map<String, Object> request = createLessonRequest(
            subjectId, TEACHER_ID, "2026-02-20", "19:20:00", "20:00:00", 1
        );

        Long lessonId = createLessonAndGetId(request, adminAccessToken);
        createdLessonIds.add(lessonId);
    }

    @Test
    @DisplayName("일반 봉사자 권한으로 수업 생성 실패(403)")
    void createLesson_forbidden_volunteer() {
        Long subjectId = createSubjectAndGetId("수학");

        Map<String, Object> request = createLessonRequest(
            subjectId, TEACHER_ID, "2026-02-21", "19:20:00", "20:00:00", 1
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
        Long subjectId = createSubjectAndGetId("영어");

        Map<String, Object> request = createLessonRequest(
            subjectId, TEACHER_ID, "2026-02-22", "19:20:00", "20:00:00", 1
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
        Long subjectId = createSubjectAndGetId("사회");

        Map<String, Object> request = createLessonRequest(
            subjectId, TEACHER_ID, "2026-02-23", "20:00:00", "19:20:00", 1
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
        Long subjectId = createSubjectAndGetId("과학");
        String date = LocalDate.of(2026, 2, 24).toString();

        // 수업 생성
        Map<String, Object> first = createLessonRequest(
            subjectId, TEACHER_ID, date, "19:00:00", "21:00:00", 1
        );
        Long firstId = createLessonAndGetId(first, adminAccessToken);
        createdLessonIds.add(firstId);

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
}
