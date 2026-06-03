package geumjeongyahak.e2e.teacher_application;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
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
@DisplayName("E2E: 관리자 교원 신청 목록 조회 테스트")
class TeacherApplicationAdminListReadTest extends BaseE2ETest {

    private static final String READ_PERMISSION = "teacher-application:read:*";
    private static final long TEST_CLASSROOM_ID = 150L;
    private static final long KOREAN_SUBJECT_ID = 150L;
    private static final long MATH_SUBJECT_ID = 151L;
    private static final long ENGLISH_SUBJECT_ID = 152L;

    private String adminToken;
    private String guestToken;
    private String readerToken;

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
        cleanupTeacherApplicationFixtures();
        insertTeacherApplicationFixtures();
        insertTeacherApplication(50L, "PENDING", "이영희", "guest01@test.com", "010-1111-1111", KOREAN_SUBJECT_ID, "2026-05-20T10:00:00");
        insertTeacherApplication(51L, "REJECTED", "김지원", "applicant-kim@test.com", "010-2222-2222", MATH_SUBJECT_ID, "2026-05-21T10:00:00");
        insertTeacherApplication(52L, "APPROVED", "박나눔", "applicant-park@test.com", "010-3333-3333", ENGLISH_SUBJECT_ID, "2026-05-22T10:00:00");

        adminToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
        guestToken = userTestHelper.generateAccessTokenByUserKey("guest01");

        User reader = userTestHelper.createTestUser("teacher-application-reader", RoleType.GUEST);
        userPermissionRepository.findByUserIdAndPermissionCode(reader.getId(), READ_PERMISSION)
            .orElseGet(() -> userPermissionRepository.save(new UserPermission(reader, READ_PERMISSION)));
        readerToken = userTestHelper.generateAccessTokenByUserKey("teacher-application-reader");
    }

    @AfterEach
    void cleanup() {
        cleanupTeacherApplications();
        cleanupTeacherApplicationFixtures();
    }

    @Test
    @DisplayName("관리자가 교원 신청 목록 조회 → 200, 기본 최신순")
    void getTeacherApplications_asAdmin_returnsPage() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get()
            .then()
            .statusCode(200)
            .body("content.id", contains(52, 51, 50))
            .body("content[0]", not(org.hamcrest.Matchers.hasKey("assignedSubjects")))
            .body("page", equalTo(0))
            .body("size", equalTo(10))
            .body("totalElements", equalTo(3))
            .body("totalPages", equalTo(1));
    }

    @Test
    @DisplayName("teacher-application:read:* 권한으로 교원 신청 목록 조회 → 200")
    void getTeacherApplications_withReadPermission_returnsPage() {
        given()
            .header(AUTH_HEADER, getAuthHeader(readerToken))
            .get()
            .then()
            .statusCode(200)
            .body("totalElements", equalTo(3));
    }

    @Test
    @DisplayName("상태로 교원 신청 목록 필터링 → 200")
    void getTeacherApplications_filterByStatus_returnsMatchingPage() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("status", "PENDING")
            .get()
            .then()
            .statusCode(200)
            .body("content.id", contains(50))
            .body("content[0].status", equalTo("PENDING"))
            .body("totalElements", equalTo(1));
    }

    @Test
    @DisplayName("검색어로 교원 신청 목록 필터링 → 200")
    void getTeacherApplications_filterByKeyword_returnsMatchingPage() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("keyword", "김지")
            .get()
            .then()
            .statusCode(200)
            .body("content.id", contains(51))
            .body("content[0].applicantName", equalTo("김지원"))
            .body("totalElements", equalTo(1));
    }

    @Test
    @DisplayName("공백 검색어는 검색 조건으로 사용하지 않는다")
    void getTeacherApplications_blankKeyword_ignoresKeywordFilter() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("keyword", "   ")
            .get()
            .then()
            .statusCode(200)
            .body("content.id", contains(52, 51, 50))
            .body("totalElements", equalTo(3));
    }

    @Test
    @DisplayName("페이지 크기로 교원 신청 목록 페이지네이션 → 200")
    void getTeacherApplications_withPageSize_returnsPagedResult() {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("page", 1)
            .queryParam("size", 2)
            .get()
            .then()
            .statusCode(200)
            .body("content.id", contains(50))
            .body("page", equalTo(1))
            .body("size", equalTo(2))
            .body("totalElements", equalTo(3))
            .body("totalPages", equalTo(2));
    }

    @Test
    @DisplayName("권한 없는 사용자가 관리자 교원 신청 목록 조회 → 403")
    void getTeacherApplications_asGuest_returns403() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .get()
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("인증 없이 관리자 교원 신청 목록 조회 → 401")
    void getTeacherApplications_unauthenticated_returns401() {
        given()
            .get()
            .then()
            .statusCode(401);
    }

    private void insertTeacherApplication(
        Long id,
        String status,
        String applicantName,
        String applicantEmail,
        String applicantPhoneNumber,
        Long preferredSubjectId,
        String createdAt
    ) {
        jdbcTemplate.update("""
            INSERT INTO teacher_applications (
                id, applicant_id, applicant_name, applicant_phone_number, applicant_email,
                birth_date, address, education_and_major, preferred_subject_id,
                motivation, desired_teacher_image, meaning_of_sharing,
                status, reviewed_at, reviewed_by, review_note, created_at, updated_at
            )
            VALUES (?, 4, ?, ?, ?,
                    '1999-03-15', '부산광역시 금정구', '부산대학교 국어국문학과 졸업', ?,
                    '지원 동기', '희망하는 교사상', '나눔의 의미',
                    ?, NULL, NULL, NULL, ?, ?)
            """,
            id,
            applicantName,
            applicantPhoneNumber,
            applicantEmail,
            preferredSubjectId,
            status,
            LocalDateTime.parse(createdAt),
            LocalDateTime.parse(createdAt)
        );
    }

    private void cleanupTeacherApplications() {
        jdbcTemplate.update("DELETE FROM teacher_applications");
    }

    private void insertTeacherApplicationFixtures() {
        jdbcTemplate.update("""
            MERGE INTO classrooms (id, name, type, description)
            KEY(id)
            VALUES (?, '교원 신청 테스트반', 'WEEKDAY', '교원 신청 목록 테스트 전용 분반')
            """, TEST_CLASSROOM_ID);

        insertSubject(KOREAN_SUBJECT_ID, "교원 신청 국어");
        insertSubject(MATH_SUBJECT_ID, "교원 신청 수학");
        insertSubject(ENGLISH_SUBJECT_ID, "교원 신청 영어");
    }

    private void insertSubject(Long subjectId, String name) {
        jdbcTemplate.update("""
            MERGE INTO subjects (
                id, class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            KEY(id)
            VALUES (
                ?, ?, NULL, ?, DATE '2026-02-01', DATE '2026-06-30', 'WEDNESDAY',
                TIME '19:20:00', TIME '20:00:00', 1, NULL, '교원 신청 목록 테스트 전용 과목', TRUE
            )
            """, subjectId, TEST_CLASSROOM_ID, name);
    }

    private void cleanupTeacherApplicationFixtures() {
        jdbcTemplate.update(
            "DELETE FROM subjects WHERE id IN (?, ?, ?)",
            KOREAN_SUBJECT_ID,
            MATH_SUBJECT_ID,
            ENGLISH_SUBJECT_ID
        );
        jdbcTemplate.update("DELETE FROM classrooms WHERE id = ?", TEST_CLASSROOM_ID);
    }
}
