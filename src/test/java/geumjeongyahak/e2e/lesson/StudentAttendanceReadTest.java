package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 학생 출석부 조회 테스트")
public class StudentAttendanceReadTest extends LessonBaseTest {

    @Test
    @DisplayName("학생 출석부 조회 성공(200 OK) - 교사(본인 수업)")
    void getStudentAttendances_Success_MyLesson() {
        long myLessonId = 1L;   // "teacher01" 의 lessonId: 1, 3

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .get("/{lessonId}/student-attendances", myLessonId)
            .then()
            .statusCode(200)
            .body("$", notNullValue())
            .body("size()", is(2))
            .body("studentId", everyItem(notNullValue()))
            .body("studentName", everyItem(notNullValue()))
            .body("status", everyItem(notNullValue()))
            .log().all();
    }

    @Test
    @DisplayName("학생 출석부 조회 실패(404/400) - 교사(타인 수업)")
    void getStudentAttendances_Fail_NotMyLesson() {
        long othersLessonId = 2L;   // "teacher02" 의 lessonId: 2

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .get("/{lessonId}/student-attendances", othersLessonId)
            .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("학생 출석부 조회 성공(200 OK) - 관리자(타인 수업도 가능)")
    void getStudentAttendances_Success_Admin_OthersLesson() {
        long othersLessonId = 2L;

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{lessonId}/student-attendances", othersLessonId)
            .then()
            .statusCode(200)
            .body("$", notNullValue())
            .body("size()", greaterThanOrEqualTo(0))
            .body("studentId", everyItem(notNullValue()))
            .body("studentName", everyItem(notNullValue()))
            .body("status", everyItem(notNullValue()))
            .log().all();
    }

    @Test
    @DisplayName("학생 출석부 조회 실패(401 Unauthorized) - 인증 없음")
    void getStudentAttendances_Unauthorized() {
        given()
            .when()
            .get("/{lessonId}/student-attendances", 1L)
            .then()
            .statusCode(401)
            .log().all();
    }
}
