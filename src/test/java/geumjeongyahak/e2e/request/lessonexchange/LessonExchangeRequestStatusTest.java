package geumjeongyahak.e2e.request.lessonexchange;

import geumjeongyahak.domain.request.repository.LessonExchangeProposalRepository;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.domain.request.service.LessonExchangeRequestService;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag("lesson-exchange-request")
@DisplayName("E2E: 수업 교환 요청 승인·반려·조회 테스트")
class LessonExchangeRequestStatusTest extends RequestBaseTest {

    @Autowired
    private LessonExchangeRequestRepository lessonExchangeRequestRepository;

    @Autowired
    private LessonExchangeProposalRepository lessonExchangeProposalRepository;

    @Autowired
    private LessonExchangeRequestService lessonExchangeRequestService;

    private final List<Long> subjectIds = new ArrayList<>();
    private final List<Long> lessonIds = new ArrayList<>();
    private final List<Long> requestIds = new ArrayList<>();
    private final List<Long> proposalIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        proposalIds.forEach(proposalId -> {
            if (lessonExchangeProposalRepository.existsById(proposalId)) {
                lessonExchangeProposalRepository.deleteById(proposalId);
            }
        });
        requestIds.forEach(requestId -> {
            if (lessonExchangeRequestRepository.existsById(requestId)) {
                lessonExchangeRequestRepository.deleteById(requestId);
            }
        });
        lessonIds.forEach(lessonId -> lessonHelper.deleteLesson(getAuthHeader(adminToken), lessonId));
        subjectIds.forEach(subjectId -> lessonHelper.deleteSubject(getAuthHeader(adminToken), subjectId));
    }

    @Test
    @DisplayName("관리자가 수업 교환 요청 승인 -> 200, APPROVED")
    void approve_asAdmin_returns200() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(5));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("processedAt", notNullValue())
            .body("processedByName", notNullValue());
    }

    @Test
    @DisplayName("매니저가 수업 교환 요청 승인 -> 200")
    void approve_asManager_returns200() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(6));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(managerToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"));
    }

    @Test
    @DisplayName("이미 처리된 요청을 다시 승인하면 -> 409")
    void approve_alreadyProcessed_returns409() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(7));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("봉사자가 승인 시도하면 -> 403")
    void approve_asVolunteer_returns403() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(8));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("매니저가 수업 교환 요청 반려 -> 200, REJECTED, note 저장")
    void reject_asManager_returns200() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(9));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(managerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "다른 운영 일정과 충돌합니다."))
            .patch("/{id}/reject", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("REJECTED"))
            .body("rejectionNote", equalTo("다른 운영 일정과 충돌합니다."));
    }

    @Test
    @DisplayName("승인 후 상세 재조회 시 processed 정보가 유지된다")
    void getDetail_afterApprove_containsProcessedInfo() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(10));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{id}", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("processedAt", notNullValue())
            .body("processedByName", equalTo("Administrator"));
    }

    @Test
    @DisplayName("반려 사유 없이 반려하면 -> 400")
    void reject_withoutNote_returns400() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(10));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of())
            .patch("/{id}/reject", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("이미 처리된 요청을 반려하면 -> 409")
    void reject_alreadyProcessed_returns409() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(11));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "뒤늦은 반려"))
            .patch("/{id}/reject", requestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("존재하지 않는 요청 승인 -> 404")
    void approve_notFound_returns404() {
        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("존재하지 않는 요청 반려 -> 404")
    void reject_notFound_returns404() {
        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "없는 요청 반려"))
            .patch("/{id}/reject", 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("자동 만료 처리 시 만료 시각이 지난 요청은 EXPIRED 로 변경된다")
    void expireExpiredRequests_changesExpiredRequestStatus() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(12));
        setRequestExpiresAt(requestId, LocalDateTime.now().minusMinutes(1));

        int expiredCount = lessonExchangeRequestService.expireExpiredLessonExchangeRequests();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(lessonExchangeRequestRepository.findById(requestId).orElseThrow().getStatus())
            .isEqualTo(LessonExchangeRequestStatus.EXPIRED);
    }

    @Test
    @DisplayName("자동 만료 처리 시 승인된 요청의 ACTIVE 제안들도 함께 CLOSED 된다")
    void expireApprovedRequest_closesActiveProposals() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(13));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(200);

        Long proposalId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("content", "만료 후 종료될 제안"))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
        proposalIds.add(proposalId);

        setRequestExpiresAt(requestId, LocalDateTime.now().minusMinutes(1));

        int expiredCount = lessonExchangeRequestService.expireExpiredLessonExchangeRequests();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(lessonExchangeRequestRepository.findById(requestId).orElseThrow().getStatus())
            .isEqualTo(LessonExchangeRequestStatus.EXPIRED);
        var proposal = lessonExchangeProposalRepository.findById(proposalId).orElseThrow();
        assertThat(proposal.getStatus().name()).isEqualTo("CLOSED");
        assertThat(proposal.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("자동 만료 처리 시 아직 만료되지 않은 요청은 그대로 유지된다")
    void expireExpiredRequests_keepsNotExpiredRequestUntouched() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(14));

        int expiredCount = lessonExchangeRequestService.expireExpiredLessonExchangeRequests();

        assertThat(expiredCount).isZero();
        assertThat(lessonExchangeRequestRepository.findById(requestId).orElseThrow().getStatus())
            .isEqualTo(LessonExchangeRequestStatus.PENDING);
    }

    @Test
    @DisplayName("자동 만료 처리 시 이미 처리된 요청은 그대로 유지된다")
    void expireExpiredRequests_keepsAlreadyProcessedRequestUntouched() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(15));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{id}/cancel", requestId)
            .then()
            .statusCode(200);

        setRequestExpiresAt(requestId, LocalDateTime.now().minusMinutes(1));

        int expiredCount = lessonExchangeRequestService.expireExpiredLessonExchangeRequests();

        assertThat(expiredCount).isZero();
        assertThat(lessonExchangeRequestRepository.findById(requestId).orElseThrow().getStatus())
            .isEqualTo(LessonExchangeRequestStatus.CANCELLED);
    }

    @Test
    @DisplayName("기본 목록 조회는 모든 인증 사용자가 전체 요청을 볼 수 있다")
    void getList_defaultView_returnsAllRequests() {
        Long volunteer1RequestId = createPendingFullRequest(
            VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(12)
        );
        Long volunteer2RequestId = createPendingFullRequest(
            VOLUNTEER2_USERNAME, TEACHER2_ID, LocalDate.now().plusDays(13)
        );

        List<Long> volunteerView = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("id", Long.class);

        assertThat(volunteerView).contains(volunteer1RequestId, volunteer2RequestId);
    }

    @Test
    @DisplayName("기본 목록 조회에서는 CANCELLED 요청이 제외된다")
    void getList_defaultView_excludesCancelledRequests() {
        Long visibleRequestId = createPendingFullRequest(
            VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(14)
        );
        Long cancelledRequestId = createPendingFullRequest(
            VOLUNTEER2_USERNAME, TEACHER2_ID, LocalDate.now().plusDays(15)
        );

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .patch("/{id}/cancel", cancelledRequestId)
            .then()
            .statusCode(200);

        List<Long> requestIds = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("id", Long.class);

        assertThat(requestIds).contains(visibleRequestId);
        assertThat(requestIds).doesNotContain(cancelledRequestId);
    }

    @Test
    @DisplayName("mine=true 이면 본인 요청만 조회된다")
    void getList_withMineTrue_returnsOnlyOwnRequests() {
        Long volunteer1RequestId = createPendingFullRequest(
            VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(16)
        );
        Long volunteer2RequestId = createPendingFullRequest(
            VOLUNTEER2_USERNAME, TEACHER2_ID, LocalDate.now().plusDays(17)
        );

        List<Long> ownView = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .queryParam("mine", true)
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("id", Long.class);

        assertThat(ownView).contains(volunteer1RequestId);
        assertThat(ownView).doesNotContain(volunteer2RequestId);
    }

    @Test
    @DisplayName("mine=true 목록 조회에서도 CANCELLED 요청은 제외된다")
    void getList_withMineTrue_excludesCancelledOwnRequest() {
        Long visibleOwnRequestId = createPendingFullRequest(
            VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(18)
        );
        Long cancelledOwnRequestId = createPendingFullRequest(
            VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(19)
        );

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{id}/cancel", cancelledOwnRequestId)
            .then()
            .statusCode(200);

        List<Long> ownView = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .queryParam("mine", true)
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("id", Long.class);

        assertThat(ownView).contains(visibleOwnRequestId);
        assertThat(ownView).doesNotContain(cancelledOwnRequestId);
    }

    @Test
    @DisplayName("status 필터로 APPROVED 요청만 조회할 수 있다")
    void getList_filteredByStatus_returnsMatchingRequests() {
        Long approvedRequestId = createPendingFullRequest(
            VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(20)
        );
        Long pendingRequestId = createPendingFullRequest(
            VOLUNTEER2_USERNAME, TEACHER2_ID, LocalDate.now().plusDays(21)
        );

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", approvedRequestId)
            .then()
            .statusCode(200);

        List<Long> approvedIds = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("status", LessonExchangeRequestStatus.APPROVED.name())
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("id", Long.class);

        assertThat(approvedIds).contains(approvedRequestId);
        assertThat(approvedIds).doesNotContain(pendingRequestId);
    }

    @Test
    @DisplayName("mine=true 와 status 를 함께 쓰면 본인 요청 중 해당 상태만 조회된다")
    void getList_filteredByMineAndStatus_returnsOwnMatchingRequests() {
        Long approvedOwnRequestId = createPendingFullRequest(
            VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(22)
        );
        Long pendingOwnRequestId = createPendingFullRequest(
            VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(23)
        );
        createPendingFullRequest(
            VOLUNTEER2_USERNAME, TEACHER2_ID, LocalDate.now().plusDays(24)
        );

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", approvedOwnRequestId)
            .then()
            .statusCode(200);

        List<Long> approvedOwnIds = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .queryParam("mine", true)
            .queryParam("status", LessonExchangeRequestStatus.APPROVED.name())
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("id", Long.class);

        assertThat(approvedOwnIds).contains(approvedOwnRequestId);
        assertThat(approvedOwnIds).doesNotContain(pendingOwnRequestId);
    }

    @Test
    @DisplayName("반려 후 status=REJECTED 목록 필터에 포함된다")
    void getList_filteredByRejectedStatus_containsRejectedRequest() {
        Long rejectedRequestId = createPendingFullRequest(
            VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(25)
        );

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(managerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "반려 후 목록 조회 테스트"))
            .patch("/{id}/reject", rejectedRequestId)
            .then()
            .statusCode(200);

        List<Long> rejectedIds = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("status", LessonExchangeRequestStatus.REJECTED.name())
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("id", Long.class);

        assertThat(rejectedIds).contains(rejectedRequestId);
    }

    @Test
    @DisplayName("status=CANCELLED 필터로 취소 요청을 직접 조회할 수 있다")
    void getList_filteredByCancelledStatus_containsCancelledRequest() {
        Long cancelledRequestId = createPendingFullRequest(
            VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(26)
        );

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{id}/cancel", cancelledRequestId)
            .then()
            .statusCode(200);

        List<Long> cancelledIds = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .queryParam("mine", true)
            .queryParam("status", LessonExchangeRequestStatus.CANCELLED.name())
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("id", Long.class);

        assertThat(cancelledIds).contains(cancelledRequestId);
    }

    @Test
    @DisplayName("다른 봉사자도 상세 조회할 수 있다")
    void getDetail_asOtherVolunteer_returns200() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(27));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .get("/{id}", requestId)
            .then()
            .statusCode(200)
            .body("id", equalTo(requestId.intValue()))
            .body("requestedByName", equalTo("홍길동"));
    }

    @Test
    @DisplayName("존재하지 않는 요청 단건 조회 -> 404")
    void getDetail_notFound_returns404() {
        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get("/{id}", 99999L)
            .then()
            .statusCode(404);
    }

    private Long createPendingFullRequest(String username, Long teacherId, LocalDate lessonDate) {
        String authHeader = VOLUNTEER_USERNAME.equals(username)
            ? getAuthHeader(volunteerToken)
            : getAuthHeader(volunteer2Token);

        Long subjectId = registerSubject(CLASSROOM_ID, teacherId);
        registerLesson(subjectId, teacherId, lessonDate, "09:00:00", "10:00:00", 1);

        Long requestId = createLessonExchangeRequest(
            authHeader,
            lessonDate,
            "수업 교환 요청",
            "해당 일정 조정이 필요합니다.",
            null,
            null,
            lessonDate.minusDays(3).atTime(23, 0)
        );
        requestIds.add(requestId);
        return requestId;
    }

    private void setRequestExpiresAt(Long requestId, LocalDateTime expiresAt) {
        var request = lessonExchangeRequestRepository.findById(requestId).orElseThrow();
        request.update(
            request.getLessonDate(),
            request.getTitle(),
            request.getClassroomNameSnapshot(),
            request.getContent(),
            request.getScope(),
            request.getStartPeriod(),
            request.getEndPeriod(),
            expiresAt
        );
        lessonExchangeRequestRepository.save(request);
    }

    private Long registerSubject(long classroomId, long teacherId) {
        Long subjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), classroomId, teacherId
        );
        subjectIds.add(subjectId);
        return subjectId;
    }

    private Long registerLesson(
        Long subjectId,
        Long teacherId,
        LocalDate lessonDate,
        String startTime,
        String endTime,
        int period
    ) {
        Long lessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken),
            subjectId,
            teacherId,
            lessonDate.toString(),
            startTime,
            endTime,
            period
        );
        lessonIds.add(lessonId);
        return lessonId;
    }
}
