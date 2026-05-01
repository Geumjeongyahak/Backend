package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: Lesson 부분 수정(PATCH) 테스트")
public class LessonUpdateTest extends LessonBaseTest {

    @Test
    @DisplayName("관리자: date/startTime/endTime만 부분 수정 성공(200)")
    void patchLesson_success_partialFields() {
        Long subjectId = createTrackedSubjectAndGetId("국어");
        Long lessonId = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-20", "19:20:00", "20:00:00", 1);

        Map<String, Object> patch = Map.of(
            "date", "2026-02-21",
            "startTime", "19:30:00",
            "endTime", "20:10:00"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{lessonId}", lessonId)
            .then()
            .statusCode(200)
            // 숫자 필드는 int로 내려올 수 있어서 intValue 비교
            .body("lessonId", equalTo(lessonId.intValue()))
            .body("date", equalTo("2026-02-21"));
    }

    @Test
    @DisplayName("관리자: startTime만 바꿔서 start>=end 되면 400")
    void patchLesson_invalidTime_400() {
        Long subjectId = createTrackedSubjectAndGetId("수학");
        Long lessonId = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-22", "19:20:00", "20:00:00", 1);

        // endTime은 기존(20:00)인데 startTime을 20:00으로 바꾸면 invalid
        Map<String, Object> patch = Map.of("startTime", "20:00:00");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{lessonId}", lessonId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("관리자: 다른 수업과 겹치게 부분 수정하면 409")
    void patchLesson_conflict_409() {
        Long subjectId = createTrackedSubjectAndGetId("영어");
        createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-23", "19:00:00", "20:00:00", 1);
        Long lessonB = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-23", "20:10:00", "21:00:00", 2);

        // B를 A와 겹치도록 start/end만 수정
        Map<String, Object> patch = Map.of(
            "startTime", "19:30:00",
            "endTime", "20:20:00"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{lessonId}", lessonB)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("봉사자: 부분 수정 실패(403)")
    void patchLesson_forbidden_403() {
        Long subjectId = createTrackedSubjectAndGetId("사회");
        Long lessonId = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-24", "19:20:00", "20:00:00", 1);

        Map<String, Object> patch = Map.of("period", 2);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body(patch)
            .when()
            .patch("/{lessonId}", lessonId)
            .then()
            .statusCode(403);
    }
}
