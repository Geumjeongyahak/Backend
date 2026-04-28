package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 교사 출석 처리 테스트")
public class LessonTeacherAttendanceTest extends LessonBaseTest {

    @Test
    @DisplayName("교사 출석 처리 성공(200 OK) - 본인 수업")
    void updateTeacherAttendance_Success_MyLesson() {
        long myLessonId = createTrackedLessonFixture(
            "attendance-my-lesson",
            TEACHER_ID,
            "2042-02-03",
            "MONDAY",
            1,
            "2027-02-03",
            "19:20:00",
            "20:00:00",
            1
        );

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
        long othersLessonId = createTrackedLessonFixture(
            "attendance-other-lesson",
            TEACHER2_ID,
            "2042-02-04",
            "TUESDAY",
            2,
            "2027-02-04",
            "20:10:00",
            "20:50:00",
            2
        );

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
        long othersLessonId = createTrackedLessonFixture(
            "attendance-admin-lesson",
            TEACHER2_ID,
            "2042-02-05",
            "WEDNESDAY",
            3,
            "2027-02-05",
            "19:20:00",
            "20:00:00",
            1
        );

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
        long myLessonId = createTrackedLessonFixture(
            "attendance-blank",
            TEACHER_ID,
            "2042-02-06",
            "THURSDAY",
            4,
            "2027-02-06",
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
            .patch("/{lessonId}/teacher-attendance", myLessonId)
            .then()
            .statusCode(400)
            .log().all();
    }
}
