package sonmoeum.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 교사 출석 처리 테스트")
public class LessonTeacherAttendanceTest extends LessonBaseTest {

    @Test
    @DisplayName("교사 출석 처리 성공(200 OK) - 본인 수업")
    void updateTeacherAttendance_Success_MyLesson() {
        long myLessonId = 1L;   // "teacher01" 의 lessonId: 1, 3

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body("""
                { "status": "PRESENT" }
            """)
            .when()
            .patch("/{lessonId}/teacher-attendance", myLessonId)
            .then()
            .statusCode(200)
            .body("lessonId", is((int) myLessonId))
            .body("teacherAttendance", is("PRESENT"))
            .log().all();
    }

    @Test
    @DisplayName("교사 출석 처리 실패(404/400) - 타인의 수업")
    void updateTeacherAttendance_Fail_NotMyLesson() {
        long othersLessonId = 2L;   // "teacher02" 의 lessonId: 2

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body("""
                { "status": "PRESENT" }
            """)
            .when()
            .patch("/{lessonId}/teacher-attendance", othersLessonId)
            .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("관리자는 교사 출석 처리 성공(200 OK) - 타인 수업도 가능")
    void updateTeacherAttendance_Success_Admin_OthersLesson() {
        long othersLessonId = 2L;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body("""
                { "status": "LATE" }
            """)
            .when()
            .patch("/{lessonId}/teacher-attendance", othersLessonId)
            .then()
            .statusCode(200)
            .body("lessonId", is((int) othersLessonId))
            .body("teacherAttendance", is("LATE"))
            .log().all();
    }

    @Test
    @DisplayName("교사 출석 처리 실패(401 Unauthorized) - 인증 없음")
    void updateTeacherAttendance_Unauthorized() {
        given()
            .contentType("application/json")
            .body("""
                { "status": "PRESENT" }
            """)
            .when()
            .patch("/{lessonId}/teacher-attendance", 1L)
            .then()
            .statusCode(401)
            .log().all();
    }

    @Test
    @DisplayName("교사 출석 처리 실패(400 Bad Request) - status 누락")
    void updateTeacherAttendance_BadRequest_StatusNull() {
        long myLessonId = 1L;

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body("""
                { }
            """)
            .when()
            .patch("/{lessonId}/teacher-attendance", myLessonId)
            .then()
            .statusCode(400)
            .log().all();
    }
}
