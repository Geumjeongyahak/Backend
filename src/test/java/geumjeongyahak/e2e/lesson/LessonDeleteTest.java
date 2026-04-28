package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: Lesson 삭제 테스트")
public class LessonDeleteTest extends LessonBaseTest {

    @Test
    @DisplayName("관리자 권한으로 수업 삭제 성공(204)")
    void deleteLesson_success_admin() {
        Long subjectId = createTrackedSubjectAndGetId("미술");
        Long lessonId = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-25", "19:20:00", "20:00:00", 1);

        // 삭제
        deleteLesson(lessonId, adminAccessToken);
    }

    @Test
    @DisplayName("일반 봉사자 권한으로 수업 삭제 실패(403)")
    void deleteLesson_forbidden_volunteer() {
        Long subjectId = createTrackedSubjectAndGetId("체육");
        Long lessonId = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-26", "19:20:00", "20:00:00", 1);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .delete("/{lessonId}", lessonId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("인증 없이 수업 삭제 실패(401)")
    void deleteLesson_unauthorized() {
        Long subjectId = createTrackedSubjectAndGetId("음악");
        Long lessonId = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-27", "19:20:00", "20:00:00", 1);

        given()
            .when()
            .delete("/{lessonId}", lessonId)
            .then()
            .statusCode(401);
    }
}
