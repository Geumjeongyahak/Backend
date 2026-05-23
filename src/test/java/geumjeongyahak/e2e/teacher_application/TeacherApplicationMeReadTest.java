package geumjeongyahak.e2e.teacher_application;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

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
@DisplayName("E2E: 내 교원 신청 조회 테스트")
class TeacherApplicationMeReadTest extends BaseE2ETest {

    private String guestToken;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/teacher-applications";
        cleanupTeacherApplications();
        guestToken = userTestHelper.generateAccessTokenByUserKey("guest01");
    }

    @AfterEach
    void cleanup() {
        cleanupTeacherApplications();
    }

    @Test
    @DisplayName("조회 가능한 교원 신청이 없으면 exists=false 반환 → 200")
    void getMyTeacherApplication_withoutVisibleApplication_returnsEmpty() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .get("/me")
            .then()
            .statusCode(200)
            .body("exists", equalTo(false))
            .body("application", nullValue());
    }

    @Test
    @DisplayName("내 PENDING 교원 신청 조회 → 200")
    void getMyTeacherApplication_withPendingApplication_returnsApplication() {
        insertTeacherApplication(10L, "PENDING", "2026-05-20T10:00:00", null);

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .get("/me")
            .then()
            .statusCode(200)
            .body("exists", equalTo(true))
            .body("application.id", equalTo(10))
            .body("application.status", equalTo("PENDING"))
            .body("application.applicantId", equalTo(4))
            .body("application.preferredSubjectId", equalTo(1));
    }

    @Test
    @DisplayName("내 REJECTED 교원 신청 조회 → 200")
    void getMyTeacherApplication_withRejectedApplication_returnsApplication() {
        insertTeacherApplication(11L, "REJECTED", "2026-05-20T10:00:00", "현재 일정과 맞지 않아 반려합니다.");

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .get("/me")
            .then()
            .statusCode(200)
            .body("exists", equalTo(true))
            .body("application.id", equalTo(11))
            .body("application.status", equalTo("REJECTED"))
            .body("application.reviewNote", equalTo("현재 일정과 맞지 않아 반려합니다."));
    }

    @Test
    @DisplayName("CANCELLED 교원 신청만 있으면 exists=false 반환 → 200")
    void getMyTeacherApplication_withOnlyCancelledApplication_returnsEmpty() {
        insertTeacherApplication(12L, "CANCELLED", "2026-05-20T10:00:00", null);

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .get("/me")
            .then()
            .statusCode(200)
            .body("exists", equalTo(false))
            .body("application", nullValue());
    }

    @Test
    @DisplayName("최신 조회 가능 교원 신청 1건 반환 → 200")
    void getMyTeacherApplication_returnsLatestVisibleApplication() {
        insertTeacherApplication(13L, "REJECTED", "2026-05-20T10:00:00", "이전 반려");
        insertTeacherApplication(14L, "PENDING", "2026-05-21T10:00:00", null);

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .get("/me")
            .then()
            .statusCode(200)
            .body("exists", equalTo(true))
            .body("application.id", equalTo(14))
            .body("application.status", equalTo("PENDING"));
    }

    @Test
    @DisplayName("인증 없이 내 교원 신청 조회 → 401")
    void getMyTeacherApplication_unauthenticated_returns401() {
        given()
            .get("/me")
            .then()
            .statusCode(401);
    }

    private void insertTeacherApplication(Long id, String status, String createdAt, String reviewNote) {
        LocalDateTime reviewedAt = "PENDING".equals(status) || "CANCELLED".equals(status)
            ? null
            : LocalDateTime.parse("2026-05-22T10:00:00");
        Long reviewedBy = reviewedAt == null ? null : 1L;

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
                    ?, ?, ?, ?, ?, ?)
            """,
            id,
            status,
            reviewedAt,
            reviewedBy,
            reviewNote,
            LocalDateTime.parse(createdAt),
            LocalDateTime.parse(createdAt)
        );
    }

    private void cleanupTeacherApplications() {
        jdbcTemplate.update("DELETE FROM teacher_applications");
    }
}
