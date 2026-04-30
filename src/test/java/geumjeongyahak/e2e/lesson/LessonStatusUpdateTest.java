package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 수업 상태 변경 테스트")
public class LessonStatusUpdateTest extends LessonBaseTest {

    @Test
    @DisplayName("수업 상태 변경 성공(200) - 교사(본인 수업) + 재조회로 반영 확인")
    void updateLessonStatus_Success_MyLesson_ThenGet() {
        long lessonId = createTrackedLessonFixture(
            "status-my-lesson",
            TEACHER_ID,
            "2042-01-06",
            "MONDAY",
            1,
            "2027-01-06",
            "19:20:00",
            "20:00:00",
            1
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body("""
                { "status": "COMPLETED" }
            """)
            .when()
            .patch("/{lessonId}/status", lessonId)
            .then()
            .statusCode(200)
            .body("lessonId", is((int) lessonId))
            .body("status", is("COMPLETED"));

        // 재조회 - DB 반영 확인
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .get("/{lessonId}", lessonId)
            .then()
            .statusCode(200)
            .body("status", is("COMPLETED"))
            .log().all();
    }

    @Test
    @DisplayName("수업 상태 변경 실패(404) - 교사(타인 수업)")
    void updateLessonStatus_Fail_OthersLesson() {
        long othersLessonId = createTrackedLessonFixture(
            "status-other-lesson",
            TEACHER2_ID,
            "2042-01-07",
            "TUESDAY",
            2,
            "2027-01-07",
            "20:10:00",
            "20:50:00",
            2
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body("""
                { "status": "COMPLETED" }
            """)
            .when()
            .patch("/{lessonId}/status", othersLessonId)
            .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("수업 상태 변경 성공(200) - 관리자(타인 수업도 가능)")
    void updateLessonStatus_Success_Admin_OthersLesson() {
        long othersLessonId = createTrackedLessonFixture(
            "status-admin-lesson",
            TEACHER2_ID,
            "2042-01-08",
            "WEDNESDAY",
            3,
            "2027-01-08",
            "19:20:00",
            "20:00:00",
            1
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body("""
                { "status": "COMPLETED" }
            """)
            .when()
            .patch("/{lessonId}/status", othersLessonId)
            .then()
            .statusCode(200)
            .body("lessonId", is((int) othersLessonId))
            .body("status", is("COMPLETED"))
            .log().all();
    }

    @Test
    @DisplayName("수업 상태 변경 실패(401) - 인증 없음")
    void updateLessonStatus_Unauthorized() {
        given()
            .contentType("application/json")
            .body("""
                { "status": "COMPLETED" }
            """)
            .when()
            .patch("/{lessonId}/status", 1L)
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("수업 상태 변경 실패(400) - status 누락")
    void updateLessonStatus_BadRequest_StatusNull() {
        long lessonId = createTrackedLessonFixture(
            "status-blank",
            TEACHER_ID,
            "2042-01-09",
            "THURSDAY",
            4,
            "2027-01-09",
            "19:20:00",
            "20:00:00",
            1
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body("""
                { }
            """)
            .when()
            .patch("/{lessonId}/status", lessonId)
            .then()
            .statusCode(400)
            .log().all();
    }
}
