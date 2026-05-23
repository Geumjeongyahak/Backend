package geumjeongyahak.e2e.teacher_application;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("teacher-application")
@DisplayName("E2E: 교원 신청 생성 테스트")
class TeacherApplicationCreateTest extends BaseE2ETest {

    private static final long UNASSIGNED_SUBJECT_ID = 100L;
    private static final long ASSIGNED_SUBJECT_ID = 1L;

    private String guestToken;
    private String volunteerToken;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/teacher-applications";
        cleanupTeacherApplicationData();
        insertUnassignedSubject();
        guestToken = userTestHelper.generateAccessTokenByUserKey("guest01");
        volunteerToken = userTestHelper.generateAccessTokenByUserKey("teacher01");
    }

    @AfterEach
    void cleanup() {
        cleanupTeacherApplicationData();
    }

    @Test
    @DisplayName("GUEST가 미배정 활성 과목으로 교원 신청 생성 → 201")
    void createTeacherApplication_asGuest_returns201() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(createRequest(UNASSIGNED_SUBJECT_ID))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("applicantId", equalTo(4))
            .body("applicantName", equalTo("이영희"))
            .body("applicantPhoneNumber", equalTo("010-1234-5678"))
            .body("applicantEmail", equalTo("guest-application@test.com"))
            .body("preferredSubjectId", equalTo((int) UNASSIGNED_SUBJECT_ID))
            .body("preferredSubjectName", equalTo("교원 신청용 미배정 과목"))
            .body("preferredClassroomName", equalTo("벚꽃반"))
            .body("status", equalTo("PENDING"));
    }

    @Test
    @DisplayName("인증 없이 교원 신청 생성 → 401")
    void createTeacherApplication_unauthenticated_returns401() {
        given()
            .contentType(ContentType.JSON)
            .body(createRequest(UNASSIGNED_SUBJECT_ID))
            .post()
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("GUEST가 아닌 사용자가 교원 신청 생성 → 403")
    void createTeacherApplication_asVolunteer_returns403() {
        given()
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(createRequest(UNASSIGNED_SUBJECT_ID))
            .post()
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("같은 사용자의 PENDING 신청이 이미 있으면 교원 신청 생성 → 409")
    void createTeacherApplication_duplicatePending_returns409() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(createRequest(UNASSIGNED_SUBJECT_ID))
            .post()
            .then()
            .statusCode(201);

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(createRequest(UNASSIGNED_SUBJECT_ID))
            .post()
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("담당 교사가 배정된 과목으로 교원 신청 생성 → 400")
    void createTeacherApplication_assignedSubject_returns400() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(createRequest(ASSIGNED_SUBJECT_ID))
            .post()
            .then()
            .statusCode(400);
    }

    private Map<String, Object> createRequest(Long preferredSubjectId) {
        return Map.ofEntries(
            entry("birthDate", "1999-03-15"),
            entry("phoneNumber", "010-1234-5678"),
            entry("email", "guest-application@test.com"),
            entry("address", "부산광역시 금정구"),
            entry("educationAndMajor", "부산대학교 국어국문학과 졸업"),
            entry("preferredSubjectId", preferredSubjectId),
            entry("motivation", "지역 교육 봉사에 관심이 생겨 지원했습니다."),
            entry("desiredTeacherImage", "꾸준히 함께 성장하는 선생님이 되고 싶습니다."),
            entry("meaningOfSharing", "제가 가진 시간을 필요한 곳에 나누는 일이라고 생각합니다.")
        );
    }

    private void insertUnassignedSubject() {
        jdbcTemplate.update("""
            INSERT INTO subjects (
                id, class_id, teacher_id, name, start_at, end_at, day_of_week,
                start_time, end_time, period, teacher_assigned_at, description, is_active
            )
            VALUES (?, 1, NULL, '교원 신청용 미배정 과목', '2026-03-01', '2026-06-30',
                    'FRIDAY', '19:00:00', '22:00:00', 1, NULL, '교원 신청 E2E 테스트 과목', TRUE)
            """, UNASSIGNED_SUBJECT_ID);
    }

    private void cleanupTeacherApplicationData() {
        jdbcTemplate.update("DELETE FROM teacher_applications");
        jdbcTemplate.update("DELETE FROM subjects WHERE id = ?", UNASSIGNED_SUBJECT_ID);
    }
}
