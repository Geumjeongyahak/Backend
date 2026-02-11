package sonmoeum.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("E2E: 학생 출석 일괄 반영 테스트")
public class StudentAttendanceBulkUpdateTest extends LessonBaseTest {

    @Test
    @DisplayName("학생 출석 일괄 반영 후 조회 시에도 반영된 값이 유지된다")
    void updateStudentAttendances_ThenGet_ShouldPersist() {
        long lessonId = 1L;     // "teacher01" 의 lessonId: 1, 3
        long studentId = 2L;

        // 일괄 반영
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body("""
                {
                  "attendances": [
                    { "studentId": 2, "status": "PRESENT", "memo": "지각 예정" }
                  ]
                }
            """)
            .when()
            .patch("/{lessonId}/student-attendances", lessonId)
            .then()
            .statusCode(200)
            // PATCH 응답에서 변경 확인
            .body("find { it.studentId == %s }.status".formatted(studentId), is("PRESENT"))
            .body("find { it.studentId == %s }.memo".formatted(studentId), is("지각 예정"));

        // 재조회 - DB 반영 여부 확인
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .when()
            .get("/{lessonId}/student-attendances", lessonId)
            .then()
            .statusCode(200)
            .body("$", notNullValue())
            .body("size()", greaterThanOrEqualTo(1))
            // GET에서도 동일하게 반영되었는지 확인
            .body("find { it.studentId == %s }.status".formatted(studentId), is("PRESENT"))
            .body("find { it.studentId == %s }.memo".formatted(studentId), is("지각 예정"))
            .log().all();
    }

    @Test
    @DisplayName("학생 출석 일괄 반영 성공(200) - 관리자는 타인 수업도 가능")
    void updateStudentAttendances_Success_Admin_OthersLesson() {
        long othersLessonId = 1L;
        long studentId = 2L;

        // 일괄 반영
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body("""
            {
              "attendances": [
                { "studentId": 2, "status": "PRESENT", "memo": "관리자 반영" }
              ]
            }
        """)
            .when()
            .patch("/{lessonId}/student-attendances", othersLessonId)
            .then()
            .statusCode(200)
            .body("$", notNullValue())
            .body("find { it.studentId == %s }.status".formatted(studentId), is("PRESENT"))
            .body("find { it.studentId == %s }.memo".formatted(studentId), is("관리자 반영"));

        // 재조회 - DB 반영 여부 확인
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .when()
            .get("/{lessonId}/student-attendances", othersLessonId)
            .then()
            .statusCode(200)
            .body("$", notNullValue())
            .body("find { it.studentId == %s }.status".formatted(studentId), is("PRESENT"))
            .body("find { it.studentId == %s }.memo".formatted(studentId), is("관리자 반영"))
            .log().all();
    }

    @Test
    @DisplayName("학생 출석 일괄 반영 실패(404) - 다른 교사의 수업")
    void updateStudentAttendances_Fail_OthersTeacherLesson() {
        long othersLessonId = 2L;   // "teacher02" 의 lessonId: 2

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body("""
            {
              "attendances": [
                { "studentId": 2, "status": "PRESENT", "memo": "권한 없음 테스트" }
              ]
            }
        """)
            .when()
            .patch("/{lessonId}/student-attendances", othersLessonId)
            .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("학생 출석 일괄 반영 실패(404) - 수업에 없는 학생")
    void updateStudentAttendances_BadRequest_StudentNotEnrolled() {
        long lessonId = 1L;

        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerAccessToken))
            .contentType("application/json")
            .body("""
                {
                  "attendances": [
                    { "studentId": 999999, "status": "PRESENT", "memo": null }
                  ]
                }
            """)
            .when()
            .patch("/{lessonId}/student-attendances", lessonId)
            .then()
            .statusCode(404)
            .log().all();
    }

    @Test
    @DisplayName("학생 출석 일괄 반영 실패(401) - 인증 없음")
    void updateStudentAttendances_Unauthorized() {
        given()
            .contentType("application/json")
            .body("""
                {
                  "attendances": [
                    { "studentId": 1, "status": "PRESENT", "memo": null }
                  ]
                }
            """)
            .when()
            .patch("/{lessonId}/student-attendances", 1L)
            .then()
            .statusCode(401);
    }
}
