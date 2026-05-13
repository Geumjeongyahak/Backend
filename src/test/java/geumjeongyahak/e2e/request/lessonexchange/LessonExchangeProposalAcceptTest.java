package geumjeongyahak.e2e.request.lessonexchange;

import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.repository.LessonExchangeProposalRepository;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag("lesson-exchange-proposal")
@DisplayName("E2E: 수업 교환 제안 수락 테스트")
class LessonExchangeProposalAcceptTest extends RequestBaseTest {

    @Autowired
    private LessonExchangeRequestRepository lessonExchangeRequestRepository;

    @Autowired
    private LessonExchangeProposalRepository lessonExchangeProposalRepository;

    @Autowired
    private LessonRepository lessonRepository;

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
    @DisplayName("요청자가 교환형 제안을 수락하면 요청 완료, 제안 수락, 다른 제안 종료, 실제 수업 교환이 반영된다")
    void acceptExchangeProposal_asRequester_returns200AndSwapsLessons() {
        LocalDate requestDate = LocalDate.now().plusDays(6);
        LocalDate proposalDate = LocalDate.now().plusDays(7);

        Long requestSubjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        Long requestLessonId = registerLesson(requestSubjectId, TEACHER_ID, requestDate, "09:00:00", "09:50:00", 1);
        Long requestId = createApprovedRequest(requestDate);

        Long proposalSubjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        Long proposalLessonId = registerLesson(proposalSubjectId, TEACHER2_ID, proposalDate, "10:00:00", "10:50:00", 2);

        Long acceptedProposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of(
                "lessonDate", proposalDate.toString(),
                "content", "교환 제안"
            )
        );
        Long closedProposalId = createProposal(
            requestId,
            getAuthHeader(managerToken),
            Map.of("content", "대체 제안")
        );
        proposalIds.add(acceptedProposalId);
        proposalIds.add(closedProposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{requestId}/proposals/{proposalId}/accept", requestId, acceptedProposalId)
            .then()
            .statusCode(200)
            .body("id", equalTo(acceptedProposalId.intValue()))
            .body("status", equalTo("ACCEPTED"))
            .body("acceptedAt", notNullValue());

        var request = lessonExchangeRequestRepository.findById(requestId).orElseThrow();
        var acceptedProposal = lessonExchangeProposalRepository.findById(acceptedProposalId).orElseThrow();
        var closedProposal = lessonExchangeProposalRepository.findById(closedProposalId).orElseThrow();

        assertThat(request.getStatus()).isEqualTo(LessonExchangeRequestStatus.COMPLETED);
        assertThat(request.getCompletedAt()).isNotNull();
        assertThat(acceptedProposal.getStatus()).isEqualTo(LessonExchangeProposalStatus.ACCEPTED);
        assertThat(acceptedProposal.getAcceptedAt()).isNotNull();
        assertThat(closedProposal.getStatus()).isEqualTo(LessonExchangeProposalStatus.CLOSED);
        assertThat(closedProposal.getClosedAt()).isNotNull();

        assertThat(lessonRepository.findById(requestLessonId).orElseThrow().getTeacher().getId())
            .isEqualTo(TEACHER2_ID);
        assertThat(lessonRepository.findById(proposalLessonId).orElseThrow().getTeacher().getId())
            .isEqualTo(TEACHER_ID);
    }

    @Test
    @DisplayName("요청자가 대체형 제안을 수락하면 요청 수업 담당 교사가 제안자로 변경되고 완료된 요청 상세 조회가 가능하다")
    void acceptSubstitutionProposal_asRequester_returns200AndChangesTeacher() {
        LocalDate requestDate = LocalDate.now().plusDays(8);

        Long requestSubjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        Long requestLessonId = registerLesson(requestSubjectId, TEACHER_ID, requestDate, "09:00:00", "09:50:00", 1);
        Long requestId = createApprovedRequest(requestDate);

        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "대체 가능합니다.")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{requestId}/proposals/{proposalId}/accept", requestId, proposalId)
            .then()
            .statusCode(200)
            .body("status", equalTo("ACCEPTED"))
            .body("proposalType", equalTo("SUBSTITUTION"))
            .body("acceptedAt", notNullValue());

        assertThat(lessonRepository.findById(requestLessonId).orElseThrow().getTeacher().getId())
            .isEqualTo(TEACHER2_ID);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{id}", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("COMPLETED"))
            .body("classroomName", equalTo("벚꽃반"));
    }

    @Test
    @DisplayName("요청자가 아닌 사용자가 제안을 수락하면 -> 403")
    void acceptProposal_asOtherUser_returns403() {
        LocalDate requestDate = LocalDate.now().plusDays(10);
        registerLesson(registerSubject(CLASSROOM_ID, TEACHER_ID), TEACHER_ID, requestDate, "09:00:00", "09:50:00", 1);
        Long requestId = createApprovedRequest(requestDate);

        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "수락 권한 없는 테스트")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(managerToken))
            .patch("/{requestId}/proposals/{proposalId}/accept", requestId, proposalId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("게스트는 수업 교환 제안을 수락할 수 없다 -> 403")
    void acceptProposal_asGuest_returns403() {
        LocalDate requestDate = LocalDate.now().plusDays(11);
        registerLesson(registerSubject(CLASSROOM_ID, TEACHER_ID), TEACHER_ID, requestDate, "09:00:00", "09:50:00", 1);
        Long requestId = createApprovedRequest(requestDate);

        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "게스트 수락 방지 테스트")
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .patch("/{requestId}/proposals/{proposalId}/accept", requestId, proposalId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("이미 비활성 상태의 제안은 수락할 수 없다 -> 409")
    void acceptWithdrawnProposal_returns409() {
        LocalDate requestDate = LocalDate.now().plusDays(11);
        registerLesson(registerSubject(CLASSROOM_ID, TEACHER_ID), TEACHER_ID, requestDate, "09:00:00", "09:50:00", 1);
        Long requestId = createApprovedRequest(requestDate);

        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of("content", "철회 후 수락 테스트")
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
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{requestId}/proposals/{proposalId}/accept", requestId, proposalId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("요청 수업 수와 제안 수업 수가 달라도 교환형 제안을 수락할 수 있다")
    void acceptExchangeProposal_withDifferentLessonCounts_returns200() {
        LocalDate requestDate = LocalDate.now().plusDays(12);
        LocalDate proposalDate = LocalDate.now().plusDays(13);

        Long requestSubjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        Long requestLessonId1 = registerLesson(requestSubjectId, TEACHER_ID, requestDate, "09:00:00", "09:50:00", 1);
        Long requestLessonId2 = registerLesson(requestSubjectId, TEACHER_ID, requestDate, "10:00:00", "10:50:00", 2);
        Long requestId = createApprovedRequest(requestDate);

        Long proposalSubjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        Long proposalLessonId = registerLesson(proposalSubjectId, TEACHER2_ID, proposalDate, "09:00:00", "09:50:00", 1);

        Long proposalId = createProposal(
            requestId,
            getAuthHeader(volunteer2Token),
            Map.of(
                "lessonDate", proposalDate.toString(),
                "content", "교시 수가 다른 교환 제안"
            )
        );
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{requestId}/proposals/{proposalId}/accept", requestId, proposalId)
            .then()
            .statusCode(200)
            .body("status", equalTo("ACCEPTED"));

        assertThat(lessonRepository.findById(requestLessonId1).orElseThrow().getTeacher().getId())
            .isEqualTo(TEACHER2_ID);
        assertThat(lessonRepository.findById(requestLessonId2).orElseThrow().getTeacher().getId())
            .isEqualTo(TEACHER2_ID);
        assertThat(lessonRepository.findById(proposalLessonId).orElseThrow().getTeacher().getId())
            .isEqualTo(TEACHER_ID);
    }

    private Long createApprovedRequest(LocalDate lessonDate) {
        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "수업 교환 요청",
            "제안 수락 테스트용 요청",
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
