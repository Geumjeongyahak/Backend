package geumjeongyahak.e2e.teacher_application;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("teacher-application")
@DisplayName("E2E: 교원 신청 관리자 화면 테스트")
@ResourceLock("teacher-application-e2e-shared-state")
class TeacherApplicationAdminViewTest extends BaseE2ETest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String sessionId;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        cleanupTeacherApplications();
        resetApplicant();
        sessionId = loginAdminSession();
    }

    @AfterEach
    void cleanup() {
        cleanupTeacherApplications();
        resetApplicant();
    }

    @Test
    @DisplayName("관리자 교원 신청 목록 화면을 조회할 수 있다")
    void teacherApplicationsPage_asAdmin_rendersList() {
        insertTeacherApplication(120L, "PENDING", "이영희", "지원 동기");

        given()
            .cookie("JSESSIONID", sessionId)
        .when()
            .get("/admin/request/teacher-applications")
        .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("교원 신청"))
            .body(containsString("이영희"))
            .body(containsString("승인 대기"));
    }

    @Test
    @DisplayName("관리자 교원 신청 상세 화면을 조회할 수 있다")
    void teacherApplicationDetailPage_asAdmin_rendersDetail() {
        insertTeacherApplication(121L, "PENDING", "이영희", "금정열린배움터에서 함께 배우고 싶습니다.");

        given()
            .cookie("JSESSIONID", sessionId)
        .when()
            .get("/admin/request/teacher-applications/{applicationId}", 121L)
        .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("이영희 교원 신청"))
            .body(containsString("금정열린배움터에서 함께 배우고 싶습니다."))
            .body(containsString("신청 처리"))
            .body(containsString("배정 분반"))
            .body(containsString("반려 사유"));
    }

    @Test
    @DisplayName("관리자 대시보드에서 교원 신청 진입점과 대기 건수를 확인할 수 있다")
    void dashboard_asAdmin_rendersTeacherApplicationEntry() {
        insertTeacherApplication(122L, "PENDING", "이영희", "지원 동기");

        given()
            .cookie("JSESSIONID", sessionId)
        .when()
            .get("/admin")
        .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("대기 중 교원 신청"))
            .body(containsString("/admin/request/teacher-applications?status=PENDING"))
            .body(containsString("<strong>1</strong>"));
    }

    @Test
    @DisplayName("관리자 화면에서 교원 신청을 승인할 수 있다")
    void approveTeacherApplicationFromAdminPage_redirectsAndApproves() {
        insertTeacherApplication(123L, "PENDING", "이영희", "지원 동기");

        given()
            .cookie("JSESSIONID", sessionId)
            .contentType(ContentType.URLENC)
            .formParam("classroomId", 2)
            .formParam("teacherStartAt", "2026-06-01")
            .formParam("teacherEndAt", "2026-12-31")
            .formParam("note", "면접 후 승인")
            .redirects()
            .follow(false)
        .when()
            .post("/admin/request/teacher-applications/{applicationId}/approve", 123L)
        .then()
            .statusCode(302)
            .header("Location", containsString("/admin/request/teacher-applications/123"));

        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM teacher_applications WHERE id = 123",
            String.class
        );
        String role = jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = 4", String.class);
        assertThat(status).isEqualTo("APPROVED");
        assertThat(role).isEqualTo("VOLUNTEER");
    }

    @Test
    @DisplayName("관리자 화면에서 교원 신청을 반려할 수 있다")
    void rejectTeacherApplicationFromAdminPage_redirectsAndRejects() {
        insertTeacherApplication(124L, "PENDING", "이영희", "지원 동기");

        given()
            .cookie("JSESSIONID", sessionId)
            .contentType(ContentType.URLENC)
            .formParam("note", "reject-note")
            .redirects()
            .follow(false)
        .when()
            .post("/admin/request/teacher-applications/{applicationId}/reject", 124L)
        .then()
            .statusCode(302)
            .header("Location", containsString("/admin/request/teacher-applications/124"));

        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM teacher_applications WHERE id = 124",
            String.class
        );
        String note = jdbcTemplate.queryForObject(
            "SELECT review_note FROM teacher_applications WHERE id = 124",
            String.class
        );
        assertThat(status).isEqualTo("REJECTED");
        assertThat(note).isEqualTo("reject-note");
    }

    private String loginAdminSession() {
        return given()
            .contentType(ContentType.URLENC)
            .formParam("username", TEST_ADMIN_EMAIL)
            .formParam("password", TEST_ADMIN_PASSWORD)
            .redirects()
            .follow(false)
        .when()
            .post("/admin/auth/login")
        .then()
            .statusCode(302)
            .extract()
            .cookie("JSESSIONID");
    }

    private void insertTeacherApplication(Long id, String status, String applicantName, String motivation) {
        jdbcTemplate.update("""
            INSERT INTO teacher_applications (
                id, applicant_id, applicant_name, applicant_phone_number, applicant_email,
                birth_date, address, education_and_major, preferred_subject_id,
                motivation, desired_teacher_image, meaning_of_sharing,
                status, reviewed_at, reviewed_by, review_note, created_at, updated_at
            )
            VALUES (?, 4, ?, '010-1234-5678', 'guest01@test.com',
                    '1999-03-15', '부산광역시 금정구', '부산대학교 국어국문학과 졸업', 1,
                    ?, '희망하는 교사상', '나눔의 의미',
                    ?, NULL, NULL, NULL, ?, ?)
            """,
            id,
            applicantName,
            motivation,
            status,
            LocalDateTime.parse("2026-05-20T10:00:00"),
            LocalDateTime.parse("2026-05-20T10:00:00")
        );
    }

    private void cleanupTeacherApplications() {
        jdbcTemplate.update("DELETE FROM teacher_applications");
    }

    private void resetApplicant() {
        jdbcTemplate.update("""
            UPDATE users
            SET role = 'GUEST',
                classroom_id = NULL,
                teacher_start_at = NULL,
                teacher_end_at = NULL
            WHERE id = 4
            """);
        jdbcTemplate.update("DELETE FROM user_permissions WHERE user_id = 4 AND permission_code LIKE 'channel:write:%'");
    }
}
