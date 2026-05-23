package geumjeongyahak.e2e.teacher_application;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.hamcrest.Matchers.equalTo;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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
@DisplayName("E2E: 교원 신청 수정 테스트")
class TeacherApplicationUpdateTest extends BaseE2ETest {

    private static final long ORIGINAL_UNASSIGNED_SUBJECT_ID = 100L;
    private static final long UPDATED_UNASSIGNED_SUBJECT_ID = 101L;
    private static final long ASSIGNED_SUBJECT_ID = 1L;

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
        cleanupTeacherApplicationData();
        insertUnassignedSubject(ORIGINAL_UNASSIGNED_SUBJECT_ID, "기존 미배정 과목");
        insertUnassignedSubject(UPDATED_UNASSIGNED_SUBJECT_ID, "수정 미배정 과목");
        guestToken = userTestHelper.generateAccessTokenByUserKey("guest01");
        otherGuest = userTestHelper.createTestUser("other-update-applicant", RoleType.GUEST);
        otherGuestToken = userTestHelper.generateAccessTokenByUserKey("other-update-applicant");
    }

    @AfterEach
    void cleanup() {
        cleanupTeacherApplicationData();
    }

    @Test
    @DisplayName("신청자 본인이 PENDING 교원 신청 수정 → 200")
    void updateTeacherApplication_asOwnerPending_returnsUpdatedApplication() {
        insertTeacherApplication(30L, 4L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(updateRequest(UPDATED_UNASSIGNED_SUBJECT_ID))
            .patch("/{applicationId}", 30L)
            .then()
            .statusCode(200)
            .body("id", equalTo(30))
            .body("applicantId", equalTo(4))
            .body("applicantName", equalTo("이영희"))
            .body("applicantPhoneNumber", equalTo("010-9999-8888"))
            .body("applicantEmail", equalTo("updated-application@test.com"))
            .body("address", equalTo("부산광역시 동래구"))
            .body("educationAndMajor", equalTo("동래대학교 교육학과 졸업"))
            .body("preferredSubjectId", equalTo((int) UPDATED_UNASSIGNED_SUBJECT_ID))
            .body("preferredSubjectName", equalTo("수정 미배정 과목"))
            .body("motivation", equalTo("수정된 지원 동기"))
            .body("status", equalTo("PENDING"));
    }

    @Test
    @DisplayName("타인의 교원 신청 수정 → 403")
    void updateTeacherApplication_asOtherUser_returns403() {
        insertTeacherApplication(31L, 4L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(otherGuestToken))
            .contentType(ContentType.JSON)
            .body(updateRequest(UPDATED_UNASSIGNED_SUBJECT_ID))
            .patch("/{applicationId}", 31L)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("PENDING이 아닌 교원 신청 수정 → 409")
    void updateTeacherApplication_notPending_returns409() {
        insertTeacherApplication(32L, 4L, "REJECTED");

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(updateRequest(UPDATED_UNASSIGNED_SUBJECT_ID))
            .patch("/{applicationId}", 32L)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("담당 교사가 배정된 과목으로 교원 신청 수정 → 400")
    void updateTeacherApplication_assignedSubject_returns400() {
        insertTeacherApplication(33L, 4L, "PENDING");

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(updateRequest(ASSIGNED_SUBJECT_ID))
            .patch("/{applicationId}", 33L)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("존재하지 않는 교원 신청 수정 → 404")
    void updateTeacherApplication_notFound_returns404() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(updateRequest(UPDATED_UNASSIGNED_SUBJECT_ID))
            .patch("/{applicationId}", 999_999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("인증 없이 교원 신청 수정 → 401")
    void updateTeacherApplication_unauthenticated_returns401() {
        given()
            .contentType(ContentType.JSON)
            .body(updateRequest(UPDATED_UNASSIGNED_SUBJECT_ID))
            .patch("/{applicationId}", 30L)
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("필수값이 비어 있으면 교원 신청 수정 → 400")
    void updateTeacherApplication_blankRequiredField_returns400() {
        insertTeacherApplication(34L, 4L, "PENDING");
        Map<String, Object> request = new java.util.HashMap<>(updateRequest(UPDATED_UNASSIGNED_SUBJECT_ID));
        request.put("motivation", " ");

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(request)
            .patch("/{applicationId}", 34L)
            .then()
            .statusCode(400);
    }

    private Map<String, Object> updateRequest(Long preferredSubjectId) {
        return Map.ofEntries(
            entry("birthDate", "1998-04-16"),
            entry("phoneNumber", "010-9999-8888"),
            entry("email", "updated-application@test.com"),
            entry("address", "부산광역시 동래구"),
            entry("educationAndMajor", "동래대학교 교육학과 졸업"),
            entry("preferredSubjectId", preferredSubjectId),
            entry("motivation", "수정된 지원 동기"),
            entry("desiredTeacherImage", "수정된 교사상"),
            entry("meaningOfSharing", "수정된 나눔의 의미")
        );
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
                    '1999-03-15', '부산광역시 금정구', '부산대학교 국어국문학과 졸업', ?,
                    '지원 동기', '희망하는 교사상', '나눔의 의미',
                    ?, NULL, NULL, NULL, ?, ?)
            """,
            id,
            applicantId,
            ORIGINAL_UNASSIGNED_SUBJECT_ID,
            status,
            LocalDateTime.parse("2026-05-20T10:00:00"),
            LocalDateTime.parse("2026-05-20T10:00:00")
        );
    }

    private void insertUnassignedSubject(Long id, String name) {
        jdbcTemplate.update("""
            INSERT INTO subjects (
                id, class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            VALUES (?, 1, NULL, ?, '2026-03-01', '2026-06-30',
                    'FRIDAY', '19:00:00', '22:00:00', 1, NULL, '교원 신청 수정 E2E 테스트 과목', TRUE)
            """, id, name);
    }

    private void cleanupTeacherApplicationData() {
        jdbcTemplate.update("DELETE FROM teacher_applications");
        jdbcTemplate.update("DELETE FROM subjects WHERE id IN (?, ?)", ORIGINAL_UNASSIGNED_SUBJECT_ID, UPDATED_UNASSIGNED_SUBJECT_ID);
    }
}
