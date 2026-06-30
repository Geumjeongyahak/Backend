package geumjeongyahak.e2e.teacher_application;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("teacher-application")
@DisplayName("E2E: 관리자 교원 신청 상세 조회 테스트")
class TeacherApplicationAdminDetailReadTest extends BaseE2ETest {

    private String adminToken;
    private String guestToken;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/admin/teacher-applications";
        cleanupTeacherApplications();
        adminToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
        guestToken = userTestHelper.generateAccessTokenByUserKey("guest01");
    }

    @AfterEach
    void cleanup() {
        cleanupTeacherApplications();
    }

    @Test
    @DisplayName("관리자가 교원 신청 상세 조회 → 200")
    void getTeacherApplication_asAdmin_returnsApplication() {
        insertTeacherApplication(20L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get("/{applicationId}", 20L)
            .then()
            .statusCode(200)
            .body("id", equalTo(20))
            .body("applicantId", equalTo(4))
            .body("applicantName", equalTo("이영희"))
            .body("preferredSubjectId", equalTo(1))
            .body("status", equalTo("PENDING"));
    }

    @Test
    @DisplayName("관리자가 CANCELLED 교원 신청 상세 조회 → 200")
    void getTeacherApplication_cancelledAsAdmin_returnsApplication() {
        insertTeacherApplication(21L, "CANCELLED");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get("/{applicationId}", 21L)
            .then()
            .statusCode(200)
            .body("id", equalTo(21))
            .body("status", equalTo("CANCELLED"));
    }

    @Test
    @DisplayName("권한 없는 사용자가 관리자 교원 신청 상세 조회 → 403")
    void getTeacherApplication_asGuest_returns403() {
        insertTeacherApplication(22L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .get("/{applicationId}", 22L)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 교원 신청 상세 조회 → 404")
    void getTeacherApplication_notFound_returns404() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get("/{applicationId}", 999_999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("인증 없이 관리자 교원 신청 상세 조회 → 401")
    void getTeacherApplication_unauthenticated_returns401() {
        given()
            .get("/{applicationId}", 20L)
            .then()
            .statusCode(401);
    }

    private void insertTeacherApplication(Long id, String status) {
        jdbcTemplate.update("""
            INSERT INTO teacher_applications (
                id, applicant_id, applicant_name, applicant_phone_number, applicant_email,
                birth_date, address, education_and_major, preferred_subject_id,
                motivation, desired_teacher_image, meaning_of_sharing,
                status, reviewed_at, reviewed_by, review_note, created_at, updated_at
            )
            VALUES (?, 4, '이영희', '010-1234-5678', 'guest01@test.com',
                    '1999-03-15', '부산광역시 금정구', '부산대학교 국어국문학과 졸업', 1,
                    '지원 동기', '희망하는 교사상', '나눔의 의미',
                    ?, NULL, NULL, NULL, ?, ?)
            """,
            id,
            status,
            LocalDateTime.parse("2026-05-20T10:00:00"),
            LocalDateTime.parse("2026-05-20T10:00:00")
        );
    }

    private void cleanupTeacherApplications() {
        jdbcTemplate.update("DELETE FROM teacher_applications");
    }
}
