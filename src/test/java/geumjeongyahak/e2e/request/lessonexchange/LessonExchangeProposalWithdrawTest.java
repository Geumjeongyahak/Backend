package geumjeongyahak.e2e.request.lessonexchange;

import geumjeongyahak.domain.request.repository.LessonExchangeProposalRepository;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag("lesson-exchange-proposal")
@DisplayName("E2E: 수업 교환 제안 철회 테스트")
class LessonExchangeProposalWithdrawTest extends RequestBaseTest {

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
    @DisplayName("제안자가 ACTIVE 제안을 철회 -> 200, WITHDRAWN")
    void withdrawActiveProposal_asProposer_returns200() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(6));
        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "철회 전 제안")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .patch("/{requestId}/proposals/{proposalId}/withdraw", requestId, proposalId)
            .then()
            .statusCode(200)
            .body("status", equalTo("WITHDRAWN"))
            .body("withdrawnAt", notNullValue());
    }

    @Test
    @DisplayName("다른 사용자가 제안을 철회하면 -> 403")
    void withdrawProposal_asOtherUser_returns403() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(7));
        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "타인 철회 방지 제안")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{requestId}/proposals/{proposalId}/withdraw", requestId, proposalId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("이미 철회된 제안을 다시 철회하면 -> 409")
    void withdrawAlreadyWithdrawnProposal_returns409() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(8));
        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "이미 철회될 제안")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .patch("/{requestId}/proposals/{proposalId}/withdraw", requestId, proposalId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .patch("/{requestId}/proposals/{proposalId}/withdraw", requestId, proposalId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("인증 없이 제안 철회 -> 401")
    void withdrawProposal_unauthenticated_returns401() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(9));
        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "인증 테스트 제안")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .patch("/{requestId}/proposals/{proposalId}/withdraw", requestId, proposalId)
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("존재하지 않는 제안 철회 -> 404")
    void withdrawProposal_notFound_returns404() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(10));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .patch("/{requestId}/proposals/{proposalId}/withdraw", requestId, 99999L)
            .then()
            .statusCode(404);
    }

    private Long createApprovedFullRequest(LocalDate lessonDate) {
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "수업 교환 요청",
            "제안 철회 테스트용 요청",
            null,
            null,
            lessonDate.minusDays(3).atTime(23, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(200);

        return requestId;
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
