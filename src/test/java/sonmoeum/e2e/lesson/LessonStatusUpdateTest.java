package sonmoeum.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 수업 상태 변경 테스트")
public class LessonStatusUpdateTest extends LessonBaseTest {

    @Test
    @DisplayName("수업 상태 변경 성공(200) - 교사(본인 수업) + 재조회로 반영 확인")
    void updateLessonStatus_Success_MyLesson_ThenGet() {
        long lessonId = 1L;

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
        long othersLessonId = 2L;

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
        long othersLessonId = 1L;

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
        long lessonId = 1L;

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
