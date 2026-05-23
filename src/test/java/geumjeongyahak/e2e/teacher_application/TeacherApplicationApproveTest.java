package geumjeongyahak.e2e.teacher_application;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("teacher-application")
@DisplayName("E2E: 관리자 교원 신청 승인 테스트")
class TeacherApplicationApproveTest extends BaseE2ETest {

    private static final String MANAGE_PERMISSION = "teacher-application:manage:*";

    private String adminToken;
    private String guestToken;
    private String managerToken;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/admin/teacher-applications";
        cleanupTeacherApplications();
        resetGuestRole();

        adminToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
        guestToken = userTestHelper.generateAccessTokenByUserKey("guest01");

        User manager = userTestHelper.createTestUser("teacher-application-manager", RoleType.GUEST);
        userPermissionRepository.findByUserIdAndPermissionCode(manager.getId(), MANAGE_PERMISSION)
            .orElseGet(() -> userPermissionRepository.save(new UserPermission(manager, MANAGE_PERMISSION)));
        managerToken = userTestHelper.generateAccessTokenByUserKey("teacher-application-manager");
    }

    @AfterEach
    void cleanup() {
        cleanupTeacherApplications();
        resetGuestRole();
    }

    @Test
    @DisplayName("관리자가 PENDING 교원 신청 승인 → 200, 신청자 VOLUNTEER 승격")
    void approveTeacherApplication_asAdmin_returnsApprovedApplication() {
        insertTeacherApplication(70L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of("note", "면접 후 승인"))
            .patch("/{applicationId}/approve", 70L)
            .then()
            .statusCode(200)
            .body("id", equalTo(70))
            .body("status", equalTo("APPROVED"))
            .body("reviewedAt", notNullValue())
            .body("reviewedByName", equalTo("관리자"))
            .body("reviewNote", equalTo("면접 후 승인"));

        String role = jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = 4", String.class);
        assertThat(role).isEqualTo("VOLUNTEER");
    }

    @Test
    @DisplayName("teacher-application:manage:* 권한으로 교원 신청 승인 → 200")
    void approveTeacherApplication_withManagePermission_returnsApprovedApplication() {
        insertTeacherApplication(71L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(managerToken))
            .contentType("application/json")
            .body(Map.of("note", "권한 승인"))
            .patch("/{applicationId}/approve", 71L)
            .then()
            .statusCode(200)
            .body("id", equalTo(71))
            .body("status", equalTo("APPROVED"))
            .body("reviewNote", equalTo("권한 승인"));
    }

    @Test
    @DisplayName("권한 없는 사용자가 교원 신청 승인 → 403")
    void approveTeacherApplication_asGuest_returns403() {
        insertTeacherApplication(72L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType("application/json")
            .body(Map.of("note", "승인"))
            .patch("/{applicationId}/approve", 72L)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("PENDING이 아닌 교원 신청 승인 → 409")
    void approveTeacherApplication_notPending_returns409() {
        insertTeacherApplication(73L, "REJECTED");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of("note", "승인"))
            .patch("/{applicationId}/approve", 73L)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("승인 시 신청자가 GUEST가 아니면 → 403")
    void approveTeacherApplication_applicantNotGuest_returns403() {
        insertTeacherApplication(74L, "PENDING");
        jdbcTemplate.update("UPDATE users SET role = 'VOLUNTEER' WHERE id = 4");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of("note", "승인"))
            .patch("/{applicationId}/approve", 74L)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 교원 신청 승인 → 404")
    void approveTeacherApplication_notFound_returns404() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of("note", "승인"))
            .patch("/{applicationId}/approve", 999_999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("인증 없이 교원 신청 승인 → 401")
    void approveTeacherApplication_unauthenticated_returns401() {
        insertTeacherApplication(75L, "PENDING");

        given()
            .contentType("application/json")
            .body(Map.of("note", "승인"))
            .patch("/{applicationId}/approve", 75L)
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

    private void resetGuestRole() {
        jdbcTemplate.update("UPDATE users SET role = 'GUEST' WHERE id = 4");
    }
}
