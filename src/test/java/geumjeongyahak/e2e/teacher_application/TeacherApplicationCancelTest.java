package geumjeongyahak.e2e.teacher_application;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
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
@DisplayName("E2E: 교원 신청 취소 테스트")
class TeacherApplicationCancelTest extends BaseE2ETest {

    private String guestToken;
    private String otherGuestToken;
    private User otherGuest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/teacher-applications";
        cleanupTeacherApplications();
        guestToken = userTestHelper.generateAccessTokenByUserKey("guest01");
        otherGuest = userTestHelper.createTestUser("other-cancel-applicant", RoleType.GUEST);
        otherGuestToken = userTestHelper.generateAccessTokenByUserKey("other-cancel-applicant");
    }

    @AfterEach
    void cleanup() {
        cleanupTeacherApplications();
    }

    @Test
    @DisplayName("신청자 본인이 PENDING 교원 신청 취소 → 204")
    void cancelTeacherApplication_asOwnerPending_returns204() {
        insertTeacherApplication(40L, 4L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .delete("/{applicationId}", 40L)
            .then()
            .statusCode(204);

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .get("/me")
            .then()
            .statusCode(200)
            .body("exists", equalTo(false))
            .body("application", nullValue());

        Integer cancelledCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM teacher_applications WHERE id = ? AND status = 'CANCELLED'",
            Integer.class,
            40L
        );
        org.assertj.core.api.Assertions.assertThat(cancelledCount).isEqualTo(1);
    }

    @Test
    @DisplayName("타인의 교원 신청 취소 → 403")
    void cancelTeacherApplication_asOtherUser_returns403() {
        insertTeacherApplication(41L, 4L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(otherGuestToken))
            .delete("/{applicationId}", 41L)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("PENDING이 아닌 교원 신청 취소 → 409")
    void cancelTeacherApplication_notPending_returns409() {
        insertTeacherApplication(42L, 4L, "REJECTED");

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .delete("/{applicationId}", 42L)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("존재하지 않는 교원 신청 취소 → 404")
    void cancelTeacherApplication_notFound_returns404() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .delete("/{applicationId}", 999_999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("인증 없이 교원 신청 취소 → 401")
    void cancelTeacherApplication_unauthenticated_returns401() {
        given()
            .delete("/{applicationId}", 40L)
            .then()
            .statusCode(401);
    }

    private void insertTeacherApplication(Long id, Long applicantId, String status) {
        jdbcTemplate.update("""
            INSERT INTO teacher_applications (
                id, applicant_id, applicant_name, applicant_phone_number, applicant_email,
                birth_date, address, education_and_major, preferred_subject_id,
                motivation, desired_teacher_image, meaning_of_sharing,
                status, reviewed_at, reviewed_by, review_note, created_at, updated_at
            )
            VALUES (?, ?, '이영희', '010-1234-5678', 'guest01@test.com',
                    '1999-03-15', '부산광역시 금정구', '부산대학교 국어국문학과 졸업', 1,
                    '지원 동기', '희망하는 교사상', '나눔의 의미',
                    ?, NULL, NULL, NULL, ?, ?)
            """,
            id,
            applicantId,
            status,
            LocalDateTime.parse("2026-05-20T10:00:00"),
            LocalDateTime.parse("2026-05-20T10:00:00")
        );
    }

    private void cleanupTeacherApplications() {
        jdbcTemplate.update("DELETE FROM teacher_applications");
    }
}
