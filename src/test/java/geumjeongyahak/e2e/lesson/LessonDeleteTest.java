package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.auth.enums.RoleType;

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
    @DisplayName("삭제된 수업은 목록과 상세 조회에서 제외된다")
    void deleteLesson_ShouldExcludeFromListAndDetail() {
        Long subjectId = createTrackedSubjectAndGetId("삭제 조회 제외");
        Long lessonId = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2028-02-25", "19:20:00", "20:00:00", 1);

        deleteLesson(lessonId, adminAccessToken);

        given()
            .queryParam("from", "2028-02-01")
            .queryParam("to", "2028-02-28")
            .when()
            .get()
            .then()
            .statusCode(200)
            .body("findAll { it.lessonId == %s }".formatted(lessonId), empty());

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{lessonId}", lessonId)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("삭제된 수업은 상태/출석/노트 변경 대상에서 제외된다")
    void deleteLesson_ShouldBlockMutations() {
        Long subjectId = createTrackedSubjectAndGetId("삭제 변경 차단");
        Long lessonId = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2028-03-25", "19:20:00", "20:00:00", 1);

        deleteLesson(lessonId, adminAccessToken);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body("""
                { "status": "COMPLETED" }
            """)
            .when()
            .patch("/{lessonId}/status", lessonId)
            .then()
            .statusCode(404);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body("""
                { "status": "PRESENT" }
            """)
            .when()
            .patch("/{lessonId}/teacher-attendance", lessonId)
            .then()
            .statusCode(404);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body("""
                { "note": "삭제된 수업 수정 시도" }
            """)
            .when()
            .put("/{lessonId}/note", lessonId)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("lesson:manage:* 권한으로 수업 삭제 성공(204)")
    void deleteLesson_success_lessonManagePermission() {
        Long subjectId = createTrackedSubjectAndGetId("매니저 삭제");
        Long lessonId = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-26", "19:20:00", "20:00:00", 1);

        String lessonManageToken = createAccessTokenWithPermission("lesson-manage-delete", RoleType.VOLUNTEER, "lesson:manage:*");
        deleteLesson(lessonId, lessonManageToken);
    }

    @Test
    @DisplayName("매니저 역할만으로 수업 삭제 실패(403)")
    void deleteLesson_forbidden_managerWithoutLessonManagePermission() {
        Long subjectId = createTrackedSubjectAndGetId("매니저 삭제 제한");
        Long lessonId = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-26", "19:20:00", "20:00:00", 1);

        given()
            .header(AUTH_HEADER, getAuthHeader(managerAccessToken))
            .when()
            .delete("/{lessonId}", lessonId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("일반 봉사자 권한으로 수업 삭제 실패(403)")
    void deleteLesson_forbidden_volunteer() {
        Long subjectId = createTrackedSubjectAndGetId("체육");
        Long lessonId = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-27", "19:20:00", "20:00:00", 1);

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
        Long lessonId = createTrackedLessonAndGetId(subjectId, TEACHER_ID, "2026-02-28", "19:20:00", "20:00:00", 1);

        given()
            .when()
            .delete("/{lessonId}", lessonId)
            .then()
            .statusCode(401);
    }
}
