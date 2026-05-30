package geumjeongyahak.e2e.meeting_record;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.meeting_record.repository.MeetingAbsenceReportRepository;
import geumjeongyahak.domain.meeting_record.repository.MeetingRecordRepository;
import geumjeongyahak.domain.meeting_record.v1.dto.request.CreateAbsenceReportRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.CreateMeetingRecordRequest;
import geumjeongyahak.domain.meeting_record.v1.dto.request.UpdateAbsenceReportRequest;
import geumjeongyahak.e2e.BaseE2ETest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("meeting-record")
@DisplayName("E2E: 교학 회의록 API")
class MeetingRecordApiTest extends BaseE2ETest {

    private static final String AUTHOR = "meeting-author@test.com";
    private static final String OTHER = "meeting-other@test.com";
    private static final String MANAGER = "meeting-manager@test.com";
    private static final String GUEST = "meeting-guest@test.com";

    private String adminToken;
    private String authorToken;
    private String otherToken;
    private String guestToken;

    @Autowired
    private MeetingRecordRepository meetingRecordRepository;

    @Autowired
    private MeetingAbsenceReportRepository absenceReportRepository;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        userTestHelper.createTestUser(AUTHOR, "회의록 작성자", "password", RoleType.VOLUNTEER);
        userTestHelper.createTestUser(OTHER, "다른 스태프", "password", RoleType.VOLUNTEER);
        userTestHelper.createTestUser(MANAGER, "매니저", "password", RoleType.MANAGER);
        userTestHelper.createTestUser(GUEST, "게스트", "password", RoleType.GUEST);

        adminToken = userTestHelper.generateAccessTokenByUserKey(TEST_ADMIN_EMAIL);
        authorToken = userTestHelper.generateAccessTokenByUserKey(AUTHOR);
        otherToken = userTestHelper.generateAccessTokenByUserKey(OTHER);
        guestToken = userTestHelper.generateAccessTokenByUserKey(GUEST);
    }

    @AfterEach
    @Override
    protected void tearDown() {
        absenceReportRepository.deleteAll();
        meetingRecordRepository.deleteAll();
        super.tearDown();
    }

    @Test
    @DisplayName("스태프는 회의록을 생성하고 상세 응답을 받을 수 있다")
    void createMeetingRecord_asStaff_returnsDetail() {
        given()
            .header(AUTH_HEADER, getAuthHeader(authorToken))
            .contentType(ContentType.JSON)
            .body(new CreateMeetingRecordRequest("5월 교학 회의", "안건"))
        .when()
            .post("/api/v1/meeting-records")
        .then()
            .statusCode(201)
            .body("title", equalTo("5월 교학 회의"))
            .body("author", equalTo("회의록 작성자"))
            .body("status", equalTo("BEFORE_MEETING"))
            .body("viewCount", equalTo(0))
            .body("agenda", equalTo("안건"))
            .body("absenceReports", hasSize(0));
    }

    @Test
    @DisplayName("GUEST는 회의록 목록을 조회할 수 없다")
    void getMeetingRecords_asGuest_returns403() {
        given()
            .header(AUTH_HEADER, getAuthHeader(guestToken))
        .when()
            .get("/api/v1/meeting-records")
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("목록 조회는 title 검색과 mineOnly 필터를 지원한다")
    void getMeetingRecords_filtersByKeywordAndMineOnly() {
        createRecord(authorToken, "나의 교학 회의", "안건");
        createRecord(otherToken, "다른 교학 회의", "안건");

        given()
            .header(AUTH_HEADER, getAuthHeader(authorToken))
            .queryParam("keyword", "교학")
            .queryParam("mineOnly", true)
            .queryParam("size", 9)
        .when()
            .get("/api/v1/meeting-records")
        .then()
            .statusCode(200)
            .body("content", hasSize(1))
            .body("content[0].title", equalTo("나의 교학 회의"))
            .body("content[0].authorId", not(equalTo(null)))
            .body("content[0].viewCount", equalTo(0));
    }

    @Test
    @DisplayName("작성자 또는 ADMIN만 회의록을 수정할 수 있고 PATCH는 상세 DTO를 반환한다")
    void updateMeetingRecord_ownerOrAdminOnly() {
        Long recordId = createRecord(authorToken, "수정 전", "안건");

        given()
            .header(AUTH_HEADER, getAuthHeader(otherToken))
            .contentType(ContentType.JSON)
            .body(Map.of("title", "권한 없음"))
        .when()
            .patch("/api/v1/meeting-records/{recordId}", recordId)
        .then()
            .statusCode(403);

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", "회의 후 회의록",
                "agenda", "수정된 안건",
                "discussion", "논의 사항",
                "suggestion", "제안 사항",
                "status", "AFTER_MEETING"
            ))
        .when()
            .patch("/api/v1/meeting-records/{recordId}", recordId)
        .then()
            .statusCode(200)
            .body("id", equalTo(recordId.intValue()))
            .body("title", equalTo("회의 후 회의록"))
            .body("agenda", equalTo("수정된 안건"))
            .body("discussion", equalTo("논의 사항"))
            .body("suggestion", equalTo("제안 사항"))
            .body("status", equalTo("AFTER_MEETING"));

        given()
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("status", "BEFORE_MEETING"))
        .when()
            .patch("/api/v1/meeting-records/{recordId}", recordId)
        .then()
            .statusCode(409)
            .body("code", equalTo("MR-005"));
    }

    @Test
    @DisplayName("AFTER_MEETING에서는 불참 사유서를 생성, 수정, 삭제할 수 없다")
    void absenceReport_afterMeeting_blocksAllWrites() {
        Long recordId = createRecord(authorToken, "회의록", "안건");
        Long reportId = createAbsenceReport(recordId, otherToken, "기존 사유", "기존 의견");

        given()
            .header(AUTH_HEADER, getAuthHeader(authorToken))
            .contentType(ContentType.JSON)
            .body(Map.of("status", "AFTER_MEETING", "discussion", "논의", "suggestion", "제안"))
        .when()
            .patch("/api/v1/meeting-records/{recordId}", recordId)
        .then()
            .statusCode(200)
            .body("status", equalTo("AFTER_MEETING"));

        given()
            .header(AUTH_HEADER, getAuthHeader(otherToken))
            .contentType(ContentType.JSON)
            .body(new CreateAbsenceReportRequest("추가 사유", null))
        .when()
            .post("/api/v1/meeting-records/{recordId}/absence-reports", recordId)
        .then()
            .statusCode(409)
            .body("code", equalTo("MR-005"));

        given()
            .header(AUTH_HEADER, getAuthHeader(otherToken))
            .contentType(ContentType.JSON)
            .body(new UpdateAbsenceReportRequest("수정 사유", "수정 의견"))
        .when()
            .patch("/api/v1/meeting-records/{recordId}/absence-reports/{reportId}", recordId, reportId)
        .then()
            .statusCode(409)
            .body("code", equalTo("MR-005"));

        given()
            .header(AUTH_HEADER, getAuthHeader(otherToken))
        .when()
            .delete("/api/v1/meeting-records/{recordId}/absence-reports/{reportId}", recordId, reportId)
        .then()
            .statusCode(409)
            .body("code", equalTo("MR-005"));
    }

    @Test
    @DisplayName("BEFORE_MEETING에서는 본인 불참 사유서만 수정 삭제할 수 있다")
    void absenceReport_beforeMeeting_ownerOnly() {
        Long recordId = createRecord(authorToken, "회의록", "안건");
        Long reportId = createAbsenceReport(recordId, otherToken, "불참", "의견");

        given()
            .header(AUTH_HEADER, getAuthHeader(authorToken))
            .contentType(ContentType.JSON)
            .body(new UpdateAbsenceReportRequest("권한 없음", null))
        .when()
            .patch("/api/v1/meeting-records/{recordId}/absence-reports/{reportId}", recordId, reportId)
        .then()
            .statusCode(403);

        given()
            .header(AUTH_HEADER, getAuthHeader(otherToken))
            .contentType(ContentType.JSON)
            .body(new UpdateAbsenceReportRequest("수정된 사유", null))
        .when()
            .patch("/api/v1/meeting-records/{recordId}/absence-reports/{reportId}", recordId, reportId)
        .then()
            .statusCode(200)
            .body("reason", equalTo("수정된 사유"))
            .body("authorId", not(equalTo(null)));

        given()
            .header(AUTH_HEADER, getAuthHeader(otherToken))
        .when()
            .delete("/api/v1/meeting-records/{recordId}/absence-reports/{reportId}", recordId, reportId)
        .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("관리자 수정 화면 조회는 조회수를 증가시키지 않는다")
    void adminEditForm_doesNotIncrementViewCount() {
        Long recordId = createRecord(authorToken, "조회수 테스트", "안건");
        String adminSessionId = loginAdminSession(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);

        given()
            .cookie("JSESSIONID", adminSessionId)
        .when()
            .get("/admin/meeting-records/{recordId}/edit", recordId)
        .then()
            .statusCode(200)
            .body(containsString("조회수 테스트"));

        assertThat(meetingRecordRepository.findById(recordId).orElseThrow().getViewCount()).isZero();
    }

    @Test
    @DisplayName("매니저는 관리자 화면으로 회의록을 조회하거나 생성할 수 없다")
    void adminView_Manager_Forbidden() {
        String managerSessionId = loginAdminSession(MANAGER, "password");

        given()
            .cookie("JSESSIONID", managerSessionId)
            .redirects()
            .follow(false)
        .when()
            .get("/admin/meeting-records")
        .then()
            .statusCode(403);

        given()
            .cookie("JSESSIONID", managerSessionId)
            .contentType(ContentType.URLENC)
            .formParam("title", "매니저 작성")
            .formParam("agenda", "안건")
            .redirects()
            .follow(false)
        .when()
            .post("/admin/meeting-records")
        .then()
            .statusCode(403);

        assertThat(meetingRecordRepository.findAll()).isEmpty();
    }

    private Long createRecord(String token, String title, String agenda) {
        return given()
            .header(AUTH_HEADER, getAuthHeader(token))
            .contentType(ContentType.JSON)
            .body(new CreateMeetingRecordRequest(title, agenda))
        .when()
            .post("/api/v1/meeting-records")
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    private Long createAbsenceReport(Long recordId, String token, String reason, String opinion) {
        return given()
            .header(AUTH_HEADER, getAuthHeader(token))
            .contentType(ContentType.JSON)
            .body(new CreateAbsenceReportRequest(reason, opinion))
        .when()
            .post("/api/v1/meeting-records/{recordId}/absence-reports", recordId)
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    private String loginAdminSession(String username, String password) {
        return given()
            .contentType(ContentType.URLENC)
            .formParam("username", username)
            .formParam("password", password)
            .redirects()
            .follow(false)
        .when()
            .post("/admin/auth/login")
        .then()
            .statusCode(302)
            .header("Location", containsString("/admin"))
            .extract()
            .cookie("JSESSIONID");
    }
}
