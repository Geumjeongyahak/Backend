package geumjeongyahak.e2e.lesson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import geumjeongyahak.domain.auth.enums.RoleType;

@DisplayName("E2E: 수업 상태 변경 테스트")
public class LessonStatusUpdateTest extends LessonBaseTest {

    @Test
    @DisplayName("수업 상태 변경 실패(403) - 교사(본인 수업)")
    void updateLessonStatus_Forbidden_VolunteerMyLesson() {
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
            .statusCode(403)
            .log().all();
    }

    @Test
    @DisplayName("수업 상태 변경 실패(403) - 매니저 역할만 보유")
    void updateLessonStatus_Forbidden_ManagerWithoutLessonManagePermission() {
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
            .header(AUTH_HEADER, getAuthHeader(managerAccessToken))
            .contentType("application/json")
            .body("""
                { "status": "COMPLETED" }
            """)
            .when()
            .patch("/{lessonId}/status", othersLessonId)
            .then()
            .statusCode(403)
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
    @DisplayName("수업 상태 변경 성공(200) - lesson:manage:* 권한")
    void updateLessonStatus_Success_LessonManagePermission() {
        long lessonId = createTrackedLessonFixture(
            "status-manage-permission",
            TEACHER2_ID,
            "2042-01-10",
            "FRIDAY",
            5,
            "2027-01-10",
            "19:20:00",
            "20:00:00",
            1
        );
        String lessonManageToken = createAccessTokenWithPermission("lesson-manage-status", RoleType.VOLUNTEER, "lesson:manage:*");

        given()
            .header(AUTH_HEADER, getAuthHeader(lessonManageToken))
            .contentType("application/json")
            .body("""
                { "status": "CANCELED" }
            """)
            .when()
            .patch("/{lessonId}/status", lessonId)
            .then()
            .statusCode(200)
            .body("lessonId", is((int) lessonId))
            .body("status", is("CANCELED"))
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
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
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

    @Test
    @DisplayName("완료된 수업을 예정 상태로 되돌릴 수 없다(400)")
    void updateLessonStatus_InvalidTransition_CompletedToScheduled() {
        long lessonId = createTrackedLessonFixture(
            "status-invalid-completed",
            TEACHER_ID,
            "2042-01-11",
            "MONDAY",
            1,
            "2027-01-11",
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
            .patch("/{lessonId}/status", lessonId)
            .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body("""
                { "status": "SCHEDULED" }
            """)
            .when()
            .patch("/{lessonId}/status", lessonId)
            .then()
            .statusCode(400)
            .body("code", is("VAL-06-002"))
            .log().all();
    }

    @Test
    @DisplayName("취소된 수업을 예정 상태로 되돌릴 수 없다(400)")
    void updateLessonStatus_InvalidTransition_CanceledToScheduled() {
        long lessonId = createTrackedLessonFixture(
            "status-invalid-canceled",
            TEACHER_ID,
            "2042-01-12",
            "TUESDAY",
            2,
            "2027-01-12",
            "20:10:00",
            "20:50:00",
            2
        );

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body("""
                { "status": "CANCELED" }
            """)
            .when()
            .patch("/{lessonId}/status", lessonId)
            .then()
            .statusCode(200);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType("application/json")
            .body("""
                { "status": "SCHEDULED" }
            """)
            .when()
            .patch("/{lessonId}/status", lessonId)
            .then()
            .statusCode(400)
            .body("code", is("VAL-06-002"))
            .log().all();
    }
}
