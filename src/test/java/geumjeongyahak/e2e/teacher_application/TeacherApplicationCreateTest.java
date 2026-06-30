package geumjeongyahak.e2e.teacher_application;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import geumjeongyahak.e2e.BaseE2ETest;
import geumjeongyahak.e2e.util.TestTeacherApplicationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("teacher-application")
@DisplayName("E2E: 교원 신청 생성 테스트")
class TeacherApplicationCreateTest extends BaseE2ETest {

    private static final long UNASSIGNED_SUBJECT_ID = 100L;
    private static final long UNASSIGNED_SECOND_SUBJECT_ID = 101L;
    private static final long ASSIGNED_SUBJECT_ID = 1L;

    private String guestToken;
    private String volunteerToken;

    @Autowired
    private TestTeacherApplicationHelper teacherApplicationHelper;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        RestAssured.basePath = "/api/v1/teacher-applications";
        cleanupTeacherApplicationData();
        insertUnassignedSubjects();
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
    @DisplayName("인증 사용자가 교원 신청 가능 시간표 목록 조회 → 미배정 과목을 분반/요일/기간 단위로 묶어 반환")
    void getAvailableTeacherSchedules_returnsGroupedUnassignedSubjects() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .get("/available-schedules")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].classroomId", equalTo(1))
            .body("[0].classroomName", equalTo("벚꽃반"))
            .body("[0].dayOfWeek", equalTo("FRIDAY"))
            .body("[0].startAt", equalTo("2026-03-01"))
            .body("[0].endAt", equalTo("2026-06-30"))
            .body("[0].startTime", equalTo("19:00:00"))
            .body("[0].endTime", equalTo("20:50:00"))
            .body("[0].subjectIds", contains((int) UNASSIGNED_SUBJECT_ID, (int) UNASSIGNED_SECOND_SUBJECT_ID))
            .body("[0].subjects.subjectId", contains((int) UNASSIGNED_SUBJECT_ID, (int) UNASSIGNED_SECOND_SUBJECT_ID));
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

    private void insertUnassignedSubjects() {
        teacherApplicationHelper.insertUnassignedSubject(
            UNASSIGNED_SUBJECT_ID,
            1L,
            "교원 신청용 미배정 과목",
            "2026-03-01",
            "2026-06-30",
            "FRIDAY",
            "19:00:00",
            "20:00:00",
            1
        );
        teacherApplicationHelper.insertUnassignedSubject(
            UNASSIGNED_SECOND_SUBJECT_ID,
            1L,
            "교원 신청용 미배정 두 번째 과목",
            "2026-03-01",
            "2026-06-30",
            "FRIDAY",
            "20:10:00",
            "20:50:00",
            2
        );
    }

    private void cleanupTeacherApplicationData() {
        teacherApplicationHelper.cleanupApplications();
        teacherApplicationHelper.cleanupSubjects(UNASSIGNED_SUBJECT_ID, UNASSIGNED_SECOND_SUBJECT_ID);
    }
}
