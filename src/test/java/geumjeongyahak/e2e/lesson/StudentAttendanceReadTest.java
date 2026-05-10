package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.auth.enums.RoleType;

@DisplayName("E2E: 학생 출석부 조회 테스트")
public class StudentAttendanceReadTest extends LessonBaseTest {

    @Test
    @DisplayName("학생 출석부 조회 성공(200 OK) - 교사(본인 수업)")
    void getStudentAttendances_Success_MyLesson() {
        long myLessonId = createTrackedLessonFixtureWithAttendances(
            "student-read-my-lesson",
            TEACHER_ID,
            "2042-03-03",
            "MONDAY",
            1,
            "2027-03-03",
            "19:20:00",
            "20:00:00",
            1,
            1L, 2L
        );

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
        long othersLessonId = createTrackedLessonFixtureWithAttendances(
            "student-read-other-lesson",
            TEACHER2_ID,
            "2042-03-04",
            "TUESDAY",
            2,
            "2027-03-04",
            "20:10:00",
            "20:50:00",
            2,
            1L, 2L
        );

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
        long othersLessonId = createTrackedLessonFixtureWithAttendances(
            "student-read-admin-lesson",
            TEACHER2_ID,
            "2042-03-05",
            "WEDNESDAY",
            3,
            "2027-03-05",
            "19:20:00",
            "20:00:00",
            1,
            1L, 2L
        );

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
    @DisplayName("학생 출석부 조회 성공(200 OK) - lesson:read:* 권한")
    void getStudentAttendances_Success_LessonReadPermission() {
        long othersLessonId = createTrackedLessonFixtureWithAttendances(
            "student-read-permission-lesson",
            TEACHER2_ID,
            "2042-03-06",
            "THURSDAY",
            4,
            "2027-03-06",
            "19:20:00",
            "20:00:00",
            1,
            1L, 2L
        );
        String lessonReadToken = createAccessTokenWithPermission(
            "lesson-read-attendance",
            RoleType.VOLUNTEER,
            "lesson:read:*"
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(lessonReadToken))
            .when()
            .get("/{lessonId}/student-attendances", othersLessonId)
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
