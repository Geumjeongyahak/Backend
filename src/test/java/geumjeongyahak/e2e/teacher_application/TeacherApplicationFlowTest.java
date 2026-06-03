package geumjeongyahak.e2e.teacher_application;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import geumjeongyahak.e2e.BaseE2ETest;
import geumjeongyahak.e2e.util.TestTeacherApplicationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("teacher-application")
@DisplayName("E2E: 교원 신청부터 시간표 배정까지 전체 플로우 테스트")
class TeacherApplicationFlowTest extends BaseE2ETest {

    private static final long FIRST_SUBJECT_ID = 190L;
    private static final long SECOND_SUBJECT_ID = 191L;

    private String adminToken;
    private String guestToken;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestTeacherApplicationHelper teacherApplicationHelper;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        cleanupFixtures();
        teacherApplicationHelper.resetApplicant(4L);
        adminToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_USERNAME);
        guestToken = userTestHelper.generateAccessTokenByUserKey("guest01");
    }

    @AfterEach
    void cleanup() {
        cleanupFixtures();
        teacherApplicationHelper.resetApplicant(4L);
    }

    @Test
    @DisplayName("available-schedules 조회 → 신청 생성 → 승인 → 사용자/시간표 배정까지 검증")
    void teacherApplicationFlow_createAndApprove_assignsSchedule() {
        insertAvailableSchedule();

        RestAssured.basePath = "/api/v1/teacher-applications";
        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
        .when()
            .get("/available-schedules")
        .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].classroomId", equalTo(2))
            .body("[0].classroomName", equalTo("장미반"))
            .body("[0].dayOfWeek", equalTo("THURSDAY"))
            .body("[0].subjectIds", contains((int) FIRST_SUBJECT_ID, (int) SECOND_SUBJECT_ID));

        Integer applicationId = given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(createRequest(FIRST_SUBJECT_ID))
        .when()
            .post()
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("status", equalTo("PENDING"))
            .extract()
            .path("id");

        RestAssured.basePath = "/api/v1/admin/teacher-applications";
        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(approveRequest())
        .when()
            .patch("/{applicationId}/approve", applicationId)
        .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("assignedSubjects.subjectId", contains((int) FIRST_SUBJECT_ID, (int) SECOND_SUBJECT_ID))
            .body("assignedClassroomId", equalTo(2))
            .body("assignedClassroomName", equalTo("장미반"))
            .body("assignedDayOfWeek", equalTo("THURSDAY"));

        Map<String, Object> user = jdbcTemplate.queryForMap("""
            SELECT role, classroom_id, teacher_start_at, teacher_end_at
            FROM users
            WHERE id = 4
            """);
        assertThat(user.get("ROLE")).isEqualTo("VOLUNTEER");
        assertThat(user.get("CLASSROOM_ID")).isEqualTo(2L);
        assertThat(user.get("TEACHER_START_AT").toString()).isEqualTo("2026-06-01");
        assertThat(user.get("TEACHER_END_AT").toString()).isEqualTo("2026-12-31");

        Integer assignedSubjectCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM subjects WHERE id IN (?, ?) AND teacher_id = 4 AND teacher_assigned_at IS NOT NULL",
            Integer.class,
            FIRST_SUBJECT_ID,
            SECOND_SUBJECT_ID
        );
        assertThat(assignedSubjectCount).isEqualTo(2);

        Integer assignmentSnapshotCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM teacher_schedule_assignments WHERE teacher_application_id = ?",
            Integer.class,
            applicationId.longValue()
        );
        assertThat(assignmentSnapshotCount).isEqualTo(2);

        Integer permissionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_permissions WHERE user_id = 4 AND permission_code = 'channel:write:5'",
            Integer.class
        );
        assertThat(permissionCount).isEqualTo(1);

        RestAssured.basePath = "/api/v1/subjects";
        given()
            .header(AUTH_HEADER, getAuthHeader(userTestHelper.generateAccessTokenByUserKey("guest01")))
        .when()
            .get("/me")
        .then()
            .statusCode(200)
            .body("id", contains((int) FIRST_SUBJECT_ID, (int) SECOND_SUBJECT_ID));
    }

    @Test
    @DisplayName("미배정 활성 시간표가 없으면 available-schedules는 빈 배열을 반환한다")
    void getAvailableTeacherSchedules_whenNoUnassignedActiveSubject_returnsEmptyArray() {
        RestAssured.basePath = "/api/v1/teacher-applications";

        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
        .when()
            .get("/available-schedules")
        .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }

    private void insertAvailableSchedule() {
        teacherApplicationHelper.insertUnassignedSubject(
            FIRST_SUBJECT_ID,
            2L,
            "전체 플로우 1교시",
            "2026-09-01",
            "2026-12-31",
            "THURSDAY",
            "19:20:00",
            "20:00:00",
            1
        );
        teacherApplicationHelper.insertUnassignedSubject(
            SECOND_SUBJECT_ID,
            2L,
            "전체 플로우 2교시",
            "2026-09-01",
            "2026-12-31",
            "THURSDAY",
            "20:10:00",
            "20:50:00",
            2
        );
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

    private Map<String, Object> approveRequest() {
        return Map.of(
            "assignedSubjectIds", List.of(FIRST_SUBJECT_ID, SECOND_SUBJECT_ID),
            "teacherStartAt", "2026-06-01",
            "teacherEndAt", "2026-12-31",
            "note", "전체 플로우 승인"
        );
    }

    private void cleanupFixtures() {
        teacherApplicationHelper.cleanupApplications();
        teacherApplicationHelper.cleanupSubjects(FIRST_SUBJECT_ID, SECOND_SUBJECT_ID);
    }
}
