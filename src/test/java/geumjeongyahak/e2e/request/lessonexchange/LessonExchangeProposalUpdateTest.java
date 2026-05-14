package geumjeongyahak.e2e.request.lessonexchange;

import geumjeongyahak.domain.request.repository.LessonExchangeProposalRepository;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

@Tag("lesson-exchange-proposal")
@DisplayName("E2E: 수업 교환 제안 수정 테스트")
class LessonExchangeProposalUpdateTest extends RequestBaseTest {

    @Autowired
    private LessonExchangeRequestRepository lessonExchangeRequestRepository;

    @Autowired
    private LessonExchangeProposalRepository lessonExchangeProposalRepository;

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
    @DisplayName("제안자가 교환형 제안을 수정 -> 200")
    void updateExchangeProposal_asProposer_returns200() {
        LocalDate requestDate = LocalDate.now().plusDays(6);
        LocalDate originalProposalDate = LocalDate.now().plusDays(7);
        LocalDate updatedProposalDate = LocalDate.now().plusDays(8);
        Long requestId = createApprovedRequest(requestDate);

        Long proposerSubjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        registerLesson(proposerSubjectId, TEACHER2_ID, originalProposalDate, "11:00:00", "11:50:00", 3);
        registerLesson(proposerSubjectId, TEACHER2_ID, updatedProposalDate, "10:00:00", "10:50:00", 2);

        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("lessonDate", originalProposalDate.toString(), "content", "기존 제안 내용")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", updatedProposalDate.toString(),
                "content", "수정된 제안 내용"
            ))
            .patch("/{requestId}/proposals/{proposalId}", requestId, proposalId)
            .then()
            .statusCode(200)
            .body("id", equalTo(proposalId.intValue()))
            .body("proposalType", equalTo("EXCHANGE"))
            .body("$", not(hasKey("proposalScope")))
            .body("lessonDate", equalTo(updatedProposalDate.toString()))
            .body("$", not(hasKey("startPeriod")))
            .body("$", not(hasKey("endPeriod")))
            .body("content", equalTo("수정된 제안 내용"))
            .body("status", equalTo("ACTIVE"));
    }

    @Test
    @DisplayName("제안자가 교환형 제안을 대체형으로 수정 -> 200")
    void updateExchangeProposal_toSubstitution_returns200() {
        LocalDate requestDate = LocalDate.now().plusDays(9);
        LocalDate proposalDate = LocalDate.now().plusDays(10);
        Long requestId = createApprovedRequest(requestDate);

        Long proposerSubjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        registerLesson(proposerSubjectId, TEACHER2_ID, proposalDate, "09:00:00", "09:50:00", 1);

        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("lessonDate", proposalDate.toString(), "content", "교환형 제안")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("content", "대체형 제안으로 수정"))
            .patch("/{requestId}/proposals/{proposalId}", requestId, proposalId)
            .then()
            .statusCode(200)
            .body("proposalType", equalTo("SUBSTITUTION"))
            .body("$", not(hasKey("proposalScope")))
            .body("lessonDate", equalTo(null))
            .body("$", not(hasKey("startPeriod")))
            .body("$", not(hasKey("endPeriod")))
            .body("classroomName", equalTo(null))
            .body("content", equalTo("대체형 제안으로 수정"));
    }

    @Test
    @DisplayName("제안자가 대체형 제안을 교환형으로 수정 -> 200")
    void updateSubstitutionProposal_toExchange_returns200() {
        LocalDate requestDate = LocalDate.now().plusDays(11);
        LocalDate proposalDate = LocalDate.now().plusDays(12);
        Long requestId = createApprovedRequest(requestDate);

        Long proposerSubjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        registerLesson(proposerSubjectId, TEACHER2_ID, proposalDate, "10:00:00", "10:50:00", 2);

        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "대체형 제안")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", proposalDate.toString(),
                "content", "교환형 제안으로 수정"
            ))
            .patch("/{requestId}/proposals/{proposalId}", requestId, proposalId)
            .then()
            .statusCode(200)
            .body("proposalType", equalTo("EXCHANGE"))
            .body("$", not(hasKey("proposalScope")))
            .body("lessonDate", equalTo(proposalDate.toString()))
            .body("$", not(hasKey("startPeriod")))
            .body("$", not(hasKey("endPeriod")))
            .body("content", equalTo("교환형 제안으로 수정"));
    }

    @Test
    @DisplayName("다른 사용자가 제안을 수정하면 -> 403")
    void updateProposal_asOtherUser_returns403() {
        Long requestId = createApprovedRequest(LocalDate.now().plusDays(11));
        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "원본 제안")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("content", "타인 수정 시도"))
            .patch("/{requestId}/proposals/{proposalId}", requestId, proposalId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("게스트는 수업 교환 제안을 수정할 수 없다 -> 403")
    void updateProposal_asGuest_returns403() {
        Long requestId = createApprovedRequest(LocalDate.now().plusDays(12));
        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "게스트 수정 방지 제안")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(Map.of("content", "게스트 수정 시도"))
            .patch("/{requestId}/proposals/{proposalId}", requestId, proposalId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("이미 비활성 상태의 제안은 수정할 수 없다 -> 409")
    void updateWithdrawnProposal_returns409() {
        Long requestId = createApprovedRequest(LocalDate.now().plusDays(12));
        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "원본 제안")
        );
        proposalIds.add(proposalId);

        var proposal = lessonExchangeProposalRepository.findById(proposalId).orElseThrow();
        proposal.withdraw();
        lessonExchangeProposalRepository.save(proposal);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("content", "수정 시도"))
            .patch("/{requestId}/proposals/{proposalId}", requestId, proposalId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("존재하지 않는 제안 수정 -> 404")
    void updateProposal_notFound_returns404() {
        Long requestId = createApprovedRequest(LocalDate.now().plusDays(13));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("content", "없는 제안 수정"))
            .patch("/{requestId}/proposals/{proposalId}", requestId, 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("교환형 제안 수정 시 요청 날짜와 같으면 -> 400")
    void updateProposal_sameDateWithRequest_returns400() {
        LocalDate requestDate = LocalDate.now().plusDays(14);
        Long requestId = createApprovedRequest(requestDate);

        Long proposerSubjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        registerLesson(proposerSubjectId, TEACHER2_ID, LocalDate.now().plusDays(15), "11:00:00", "11:50:00", 3);
        registerLesson(proposerSubjectId, TEACHER2_ID, requestDate, "09:00:00", "09:50:00", 1);

        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of(
                "lessonDate", LocalDate.now().plusDays(15).toString(),
                "content", "겹치지 않는 제안"
            )
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", requestDate.toString(),
                "content", "같은 날짜 제안으로 수정"
            ))
            .patch("/{requestId}/proposals/{proposalId}", requestId, proposalId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("교환형 제안 수정 시 실제 제안 수업이 없으면 -> 400")
    void updateProposal_withoutProposalLessons_returns400() {
        Long requestId = createApprovedRequest(LocalDate.now().plusDays(15));
        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "대체형 제안")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", LocalDate.now().plusDays(16).toString(),
                "content", "수업이 없는 날짜로 수정"
            ))
            .patch("/{requestId}/proposals/{proposalId}", requestId, proposalId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("만료된 요청에 달린 제안 수정 시도 -> 409")
    void updateProposal_onExpiredRequest_returns409() {
        Long requestId = createApprovedRequest(LocalDate.now().plusDays(21));
        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "원본 제안")
        );
        proposalIds.add(proposalId);

        var request = lessonExchangeRequestRepository.findById(requestId).orElseThrow();
        request.update(
            request.getLessonDate(),
            request.getTitle(),
            request.getClassroomNameSnapshot(),
            request.getContent(),
            LocalDateTime.now().minusMinutes(1)
        );
        lessonExchangeRequestRepository.save(request);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("content", "만료 후 수정 시도"))
            .patch("/{requestId}/proposals/{proposalId}", requestId, proposalId)
            .then()
            .statusCode(409);
    }

    private Long createApprovedRequest(LocalDate lessonDate) {
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "수업 교환 요청",
            "제안 수정 테스트용 요청",
            lessonDate.minusDays(3).atTime(23, 0)
        );
        requestIds.add(requestId);
        approveRequest(requestId);
        return requestId;
    }

    private void approveRequest(Long requestId) {
        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(200);
    }

    private Long createProposal(Long requestId, String authHeader, Map<String, Object> body) {
        return given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, authHeader)
            .contentType(ContentType.JSON)
            .body(body)
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
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
