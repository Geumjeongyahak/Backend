package geumjeongyahak.e2e.request.absence;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.request.repository.AbsenceRequestRepository;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import geumjeongyahak.domain.users.repository.UserPermissionRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;

/**
 * 결석 요청 조회 E2E 테스트.
 *
 * <h3>격리 전략</h3>
 * @BeforeEach 에서 독립 수업 + 결석 요청을 생성하고, @AfterEach 에서 삭제한다.
 * 관리자 전체 조회 / 봉사자 본인 조회 / 상태 필터 / 단건 조회 / 권한 오류를 검증한다.
 */
@Tag("absence-request")
@DisplayName("E2E: 결석 요청 조회 테스트")
class AbsenceRequestReadTest extends RequestBaseTest {

    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    private Long subjectId;
    private Long lessonIdForVolunteer1;
    private Long lessonIdForVolunteer2;
    private Long requestIdByVolunteer1;
    private Long requestIdByVolunteer2;
    private String readPermissionToken;

    /** 각 테스트마다 독립 수업 2개 + 요청 2개(각 봉사자 1개씩) 생성 */
    @BeforeEach
    void createTestRequests() {
        subjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);

        lessonIdForVolunteer1 = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), subjectId, TEACHER_ID);
        lessonIdForVolunteer2 = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), subjectId, TEACHER2_ID);

        requestIdByVolunteer1 = createAbsenceRequest(
            getAuthHeader(volunteerToken), lessonIdForVolunteer1, "봉사자1 결석 요청", "봉사자1 결석 사유");
        requestIdByVolunteer2 = createAbsenceRequest(
            getAuthHeader(volunteer2Token), lessonIdForVolunteer2, "봉사자2 결석 요청", "봉사자2 결석 사유");

        User readPermissionUser = userTestHelper.createTestUser("absence-reader", RoleType.GUEST);
        userPermissionRepository.findByUserIdAndPermissionCode(
                readPermissionUser.getId(), "absence-request:read:*")
            .orElseGet(() -> userPermissionRepository.save(
                new UserPermission(readPermissionUser, "absence-request:read:*")));
        readPermissionToken = userTestHelper.generateAccessTokenByNickname("absence-reader");
    }

    @AfterEach
    void cleanup() {
        if (requestIdByVolunteer1 != null) absenceRequestRepository.deleteById(requestIdByVolunteer1);
        if (requestIdByVolunteer2 != null) absenceRequestRepository.deleteById(requestIdByVolunteer2);
        if (lessonIdForVolunteer1 != null) lessonHelper.deleteLesson(getAuthHeader(adminToken), lessonIdForVolunteer1);
        if (lessonIdForVolunteer2 != null) lessonHelper.deleteLesson(getAuthHeader(adminToken), lessonIdForVolunteer2);
        if (subjectId != null) lessonHelper.deleteSubject(getAuthHeader(adminToken), subjectId);
    }

    // ── 목록 조회 ─────────────────────────────────────────

    @Test
    @DisplayName("관리자 전체 목록 조회 → 두 요청 모두 포함")
    void getList_asAdmin_seesAllRequests() {
        List<Long> ids = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("content.id", Long.class);

        assertThat(ids).contains(requestIdByVolunteer1, requestIdByVolunteer2);
    }

    @Test
    @DisplayName("목록 조회 기본 페이지 응답 → page=0, size=10")
    void getList_defaultPagination_returnsPageMetadata() {
        List<String> titles = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get()
            .then()
            .statusCode(200)
            .body("page", equalTo(0))
            .body("size", equalTo(10))
            .body("totalElements", greaterThanOrEqualTo(2))
            .body("totalPages", greaterThanOrEqualTo(1))
            .extract()
            .jsonPath()
            .getList("content.title", String.class);

        assertThat(titles).contains("봉사자1 결석 요청", "봉사자2 결석 요청");
    }

    @Test
    @DisplayName("목록 조회 page/size 적용 → 요청한 페이지 크기로 반환")
    void getList_withPageAndSize_returnsRequestedPage() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("page", 0)
            .queryParam("size", 1)
            .get()
            .then()
            .statusCode(200)
            .body("page", equalTo(0))
            .body("size", equalTo(1))
            .body("content.size()", equalTo(1));
    }

    @Test
    @DisplayName("매니저 목록 조회 → 본인 요청 범위만 조회")
    void getList_asManager_seesOnlyOwnRequests() {
        List<Long> ids = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(managerToken))
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("content.id", Long.class);

        assertThat(ids).doesNotContain(requestIdByVolunteer1, requestIdByVolunteer2);
    }

    @Test
    @DisplayName("absence-request:read:* 권한자 목록 조회 → 두 요청 모두 포함")
    void getList_withReadPermission_seesAllRequests() {
        List<Long> ids = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(readPermissionToken))
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("content.id", Long.class);

        assertThat(ids).contains(requestIdByVolunteer1, requestIdByVolunteer2);
    }

    @Test
    @DisplayName("봉사자1 목록 조회 → 본인 요청만 포함, 봉사자2 요청 미포함")
    void getList_asVolunteer_seesOnlyOwn() {
        List<Long> ids = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("content.id", Long.class);

        assertThat(ids).contains(requestIdByVolunteer1);
        assertThat(ids).doesNotContain(requestIdByVolunteer2);
    }

    @Test
    @DisplayName("status=PENDING 필터 → 두 요청 모두 포함(둘 다 PENDING 상태)")
    void getList_filteredByPendingStatus_containsBothRequests() {
        List<Long> ids = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("status", "PENDING")
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("content.id", Long.class);

        assertThat(ids).contains(requestIdByVolunteer1, requestIdByVolunteer2);
    }

    @Test
    @DisplayName("status=APPROVED 필터 → PENDING 요청 미포함")
    void getList_filteredByApprovedStatus_doesNotContainPendingRequests() {
        List<Long> ids = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("status", "APPROVED")
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("content.id", Long.class);

        assertThat(ids).doesNotContain(requestIdByVolunteer1, requestIdByVolunteer2);
    }

    @Test
    @DisplayName("인증 없이 목록 조회 → 401")
    void getList_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/absence-requests")
            .get()
            .then()
            .statusCode(401);
    }

    // ── 단건 조회 ─────────────────────────────────────────

    @Test
    @DisplayName("관리자 단건 조회 → 200, 필드 검증")
    void getDetail_asAdmin_returns200() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get("/{id}", requestIdByVolunteer1)
            .then()
            .statusCode(200)
            .body("id", equalTo(requestIdByVolunteer1.intValue()))
            .body("classroomId", equalTo((int) CLASSROOM_ID))
            .body("classroomName", equalTo("벚꽃반"))
            .body("status", equalTo("PENDING"))
            .body("title", equalTo("봉사자1 결석 요청"))
            .body("reason", equalTo("봉사자1 결석 사유"));
    }

    @Test
    @DisplayName("매니저가 타인 요청 단건 조회 → 403")
    void getDetail_asManagerForOthersRequest_returns403() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(managerToken))
            .get("/{id}", requestIdByVolunteer1)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("absence-request:read:* 권한자 단건 조회 → 200")
    void getDetail_withReadPermission_returns200() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(readPermissionToken))
            .get("/{id}", requestIdByVolunteer1)
            .then()
            .statusCode(200)
            .body("id", equalTo(requestIdByVolunteer1.intValue()));
    }

    @Test
    @DisplayName("요청 소유자(봉사자1) 단건 조회 → 200")
    void getDetail_asOwner_returns200() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{id}", requestIdByVolunteer1)
            .then()
            .statusCode(200)
            .body("id", equalTo(requestIdByVolunteer1.intValue()));
    }

    @Test
    @DisplayName("타인(봉사자2)이 봉사자1 요청 단건 조회 → 403")
    void getDetail_asOtherVolunteer_returns403() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .get("/{id}", requestIdByVolunteer1)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 요청 ID 단건 조회 → 404")
    void getDetail_notFound_returns404() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get("/{id}", 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("인증 없이 단건 조회 → 401")
    void getDetail_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/absence-requests")
            .get("/{id}", requestIdByVolunteer1)
            .then()
            .statusCode(401);
    }
}
