package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 수업일지(메모) 테스트")
public class LessonNoteE2ETest extends LessonBaseTest {

    @Test
    @DisplayName("교사: 수업일지 저장 후 조회 시 반영된다(200)")
    void upsertAndGetNote_Teacher_MyLesson() {
        long lessonId = 1L;

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body("""
                { "note": "오늘 진도: 1과 / 특이사항 없음" }
            """)
            .when()
            .put("/{lessonId}/note", lessonId)
            .then()
            .statusCode(200)
            .body("lessonId", is((int) lessonId))
            .body("note", notNullValue());

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .get("/{lessonId}/note", lessonId)
            .then()
            .statusCode(200)
            .body("lessonId", is((int) lessonId))
            .body("note", is("오늘 진도: 1과 / 특이사항 없음"));
    }

    @Test
    @DisplayName("교사: 타인 수업 메모 조회/수정 불가(404)")
    void note_Teacher_OthersLesson_ForbiddenAsNotFound() {
        long othersLessonId = 2L;

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .get("/{lessonId}/note", othersLessonId)
            .then()
            .statusCode(404);

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body("""
                { "note": "수정 시도" }
            """)
            .when()
            .put("/{lessonId}/note", othersLessonId)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("관리자: 타인 수업 메모 저장/조회 가능(200)")
    void note_Admin_OthersLesson_Success() {
        long othersLessonId = 1L;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body("""
                { "note": "관리자 메모" }
            """)
            .when()
            .put("/{lessonId}/note", othersLessonId)
            .then()
            .statusCode(200)
            .body("lessonId", is((int) othersLessonId))
            .body("note", is("관리자 메모"));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{lessonId}/note", othersLessonId)
            .then()
            .statusCode(200)
            .body("note", is("관리자 메모"));
    }

    @Test
    @DisplayName("메모 저장 실패(400) - note 공백")
    void upsertNote_BadRequest_BlankNote() {
        long lessonId = 1L;

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body("""
                { "note": "   " }
            """)
            .when()
            .put("/{lessonId}/note", lessonId)
            .then()
            .statusCode(400);
    }
}
