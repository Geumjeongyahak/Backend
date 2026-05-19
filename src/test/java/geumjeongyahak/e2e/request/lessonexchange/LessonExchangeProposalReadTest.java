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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Tag("lesson-exchange-proposal")
@DisplayName("E2E: 수업 교환 제안 조회 테스트")
class LessonExchangeProposalReadTest extends RequestBaseTest {

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
    @DisplayName("특정 요청의 제안 목록 조회 -> 200, 최신순 반환")
    void getProposals_returns200InDescOrder() {
        LocalDate requestDate = LocalDate.now().plusDays(6);
        LocalDate proposalDate = LocalDate.now().plusDays(7);
        Long requestId = createApprovedFullRequest(requestDate);

        Long proposerSubjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        registerLesson(proposerSubjectId, TEACHER2_ID, proposalDate, "09:00:00", "09:50:00", 1);

        Long exchangeProposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of(
                "lessonDate", proposalDate.toString(),
                "content", "다음 날 전체 수업으로 교환 가능합니다."
            )
        );
        Long substitutionProposalId = createProposal(
            requestId,
            getAuthHeader(managerToken),
            Map.of("content", "대체 수업 가능합니다.")
        );
        proposalIds.add(exchangeProposalId);
        proposalIds.add(substitutionProposalId);

        List<Integer> ids = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}/proposals", requestId)
            .then()
            .statusCode(200)
            .body("[0].id", equalTo(substitutionProposalId.intValue()))
            .body("[0].proposalType", equalTo("SUBSTITUTION"))
            .body("[0].classroomName", equalTo(null))
            .body("[1].id", equalTo(exchangeProposalId.intValue()))
            .body("[1].proposalType", equalTo("EXCHANGE"))
            .body("[1].classroomName", equalTo("벚꽃반"))
            .extract()
            .jsonPath()
            .getList("id", Integer.class);

        assertThat(ids).hasSize(2);
    }

    @Test
    @DisplayName("제안이 없는 요청의 목록 조회 -> 200, 빈 배열")
    void getProposals_withoutProposals_returnsEmptyList() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(8));

        List<Integer> ids = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}/proposals", requestId)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("id", Integer.class);

        assertThat(ids).isEmpty();
    }

    @Test
    @DisplayName("철회된 제안은 목록 조회에서 제외된다")
    void getProposals_excludesWithdrawnProposal() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(9));
        Long activeProposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "보이는 제안")
        );
        Long withdrawnProposalId = createProposal(
            requestId,
            getAuthHeader(managerToken),
            Map.of("content", "철회될 제안")
        );
        proposalIds.add(activeProposalId);
        proposalIds.add(withdrawnProposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(managerToken))
            .patch("/{requestId}/proposals/{proposalId}/withdraw", requestId, withdrawnProposalId)
            .then()
            .statusCode(200);

        List<Long> ids = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}/proposals", requestId)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("id", Long.class);

        assertThat(ids).contains(activeProposalId);
        assertThat(ids).doesNotContain(withdrawnProposalId);
    }

    @Test
    @DisplayName("인증 없이 제안 목록 조회 -> 401")
    void getProposals_unauthenticated_returns401() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(10));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .get("/{requestId}/proposals", requestId)
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("게스트는 수업 교환 제안 목록을 조회할 수 없다 -> 403")
    void getProposals_asGuest_returns403() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(10));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .get("/{requestId}/proposals", requestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 요청의 제안 목록 조회 -> 404")
    void getProposals_requestNotFound_returns404() {
        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}/proposals", 99999L)
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
            "제안 조회 테스트용 요청",
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
