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
@DisplayName("E2E: 관리자 교원 신청 반려 테스트")
class TeacherApplicationRejectTest extends BaseE2ETest {

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

        User manager = userTestHelper.createTestUser("teacher-application-reject-manager", RoleType.GUEST);
        userPermissionRepository.findByUserIdAndPermissionCode(manager.getId(), MANAGE_PERMISSION)
            .orElseGet(() -> userPermissionRepository.save(new UserPermission(manager, MANAGE_PERMISSION)));
        managerToken = userTestHelper.generateAccessTokenByUserKey("teacher-application-reject-manager");
    }

    @AfterEach
    void cleanup() {
        cleanupTeacherApplications();
        resetGuestRole();
    }

    @Test
    @DisplayName("관리자가 PENDING 교원 신청 반려 → 200, 신청자 GUEST 유지")
    void rejectTeacherApplication_asAdmin_returnsRejectedApplication() {
        insertTeacherApplication(80L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of("note", "면접 일정 미참석"))
            .patch("/{applicationId}/reject", 80L)
            .then()
            .statusCode(200)
            .body("id", equalTo(80))
            .body("status", equalTo("REJECTED"))
            .body("reviewedAt", notNullValue())
            .body("reviewedByName", equalTo("관리자"))
            .body("reviewNote", equalTo("면접 일정 미참석"));

        String role = jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = 4", String.class);
        assertThat(role).isEqualTo("GUEST");
    }

    @Test
    @DisplayName("teacher-application:manage:* 권한으로 교원 신청 반려 → 200")
    void rejectTeacherApplication_withManagePermission_returnsRejectedApplication() {
        insertTeacherApplication(81L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(managerToken))
            .contentType("application/json")
            .body(Map.of("note", "서류 보완 필요"))
            .patch("/{applicationId}/reject", 81L)
            .then()
            .statusCode(200)
            .body("id", equalTo(81))
            .body("status", equalTo("REJECTED"))
            .body("reviewNote", equalTo("서류 보완 필요"));
    }

    @Test
    @DisplayName("반려 사유 없이 교원 신청 반려 → 400")
    void rejectTeacherApplication_blankNote_returns400() {
        insertTeacherApplication(82L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of("note", ""))
            .patch("/{applicationId}/reject", 82L)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("권한 없는 사용자가 교원 신청 반려 → 403")
    void rejectTeacherApplication_asGuest_returns403() {
        insertTeacherApplication(83L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType("application/json")
            .body(Map.of("note", "반려"))
            .patch("/{applicationId}/reject", 83L)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("PENDING이 아닌 교원 신청 반려 → 409")
    void rejectTeacherApplication_notPending_returns409() {
        insertTeacherApplication(84L, "APPROVED");

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of("note", "반려"))
            .patch("/{applicationId}/reject", 84L)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("존재하지 않는 교원 신청 반려 → 404")
    void rejectTeacherApplication_notFound_returns404() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType("application/json")
            .body(Map.of("note", "반려"))
            .patch("/{applicationId}/reject", 999_999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("인증 없이 교원 신청 반려 → 401")
    void rejectTeacherApplication_unauthenticated_returns401() {
        insertTeacherApplication(85L, "PENDING");

        given()
            .contentType("application/json")
            .body(Map.of("note", "반려"))
            .patch("/{applicationId}/reject", 85L)
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
