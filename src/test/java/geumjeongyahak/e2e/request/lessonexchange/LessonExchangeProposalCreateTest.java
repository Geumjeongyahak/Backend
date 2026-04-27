package geumjeongyahak.e2e.request.lessonexchange;

import geumjeongyahak.domain.request.enums.LessonExchangeScope;
import geumjeongyahak.domain.request.repository.LessonExchangeProposalRepository;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag("lesson-exchange-proposal")
@DisplayName("E2E: 수업 교환 제안 생성 테스트")
class LessonExchangeProposalCreateTest extends RequestBaseTest {

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
    @DisplayName("승인된 요청에 대해 교환형 전체 제안 생성 -> 201")
    void createExchangeProposal_fullScope_returns201() {
        LocalDate requestDate = LocalDate.now().plusDays(5);
        LocalDate proposalDate = LocalDate.now().plusDays(6);
        Long requestId = createApprovedFullRequest(requestDate);

        Long proposerSubjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        registerLesson(proposerSubjectId, TEACHER2_ID, proposalDate, "09:00:00", "10:00:00", 1);

        Long proposalId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", proposalDate.toString(),
                "content", "다음 날 전체 수업으로 교환 가능합니다."
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("requestId", equalTo(requestId.intValue()))
            .body("proposalType", equalTo("EXCHANGE"))
            .body("proposalScope", equalTo("FULL"))
            .body("lessonDate", equalTo(proposalDate.toString()))
            .body("proposedByName", equalTo("김철수"))
            .body("status", equalTo("ACTIVE"))
            .extract()
            .jsonPath()
            .getLong("id");

        proposalIds.add(proposalId);
    }

    @Test
    @DisplayName("승인된 요청에 대해 대체형 제안 생성 -> 201")
    void createSubstitutionProposal_returns201() {
        LocalDate requestDate = LocalDate.now().plusDays(7);
        Long requestId = createApprovedFullRequest(requestDate);

        Long proposalId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "content", "해당 날짜는 대체 수업으로 지원 가능합니다."
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(201)
            .body("proposalType", equalTo("SUBSTITUTION"))
            .body("proposalScope", equalTo(null))
            .body("lessonDate", equalTo(null))
            .body("startPeriod", equalTo(null))
            .body("endPeriod", equalTo(null))
            .extract()
            .jsonPath()
            .getLong("id");

        proposalIds.add(proposalId);
    }

    @Test
    @DisplayName("승인된 부분 요청에 대해 같은 날짜의 비겹침 PARTIAL 제안 생성 -> 201")
    void createExchangeProposal_partialNonOverlapping_returns201() {
        LocalDate requestDate = LocalDate.now().plusDays(8);
        Long requestId = createApprovedPartialRequest(requestDate, 1, 2);

        Long proposerSubjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        registerLesson(proposerSubjectId, TEACHER2_ID, requestDate, "11:00:00", "11:50:00", 3);

        Long proposalId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", requestDate.toString(),
                "startPeriod", 3,
                "endPeriod", 3,
                "content", "같은 날짜 3교시로 교환 가능합니다."
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(201)
            .body("proposalType", equalTo("EXCHANGE"))
            .body("proposalScope", equalTo("PARTIAL"))
            .body("startPeriod", equalTo(3))
            .body("endPeriod", equalTo(3))
            .extract()
            .jsonPath()
            .getLong("id");

        proposalIds.add(proposalId);
    }

    @Test
    @DisplayName("인증 없이 수업 교환 제안 생성 -> 401")
    void createProposal_unauthenticated_returns401() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(8));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .contentType(ContentType.JSON)
            .body(Map.of("content", "무인증 제안"))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("자신의 요청에는 제안할 수 없다 -> 403")
    void createProposal_toOwnRequest_returns403() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(9));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("content", "내 요청에 직접 제안"))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("승인되지 않은 요청에는 제안할 수 없다 -> 409")
    void createProposal_toPendingRequest_returns409() {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, LocalDate.now().plusDays(10));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("content", "승인 전 요청 제안"))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("만료된 APPROVED 요청에는 제안할 수 없다 -> 409")
    void createProposal_toExpiredApprovedRequest_returns409() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(10));

        var request = lessonExchangeRequestRepository.findById(requestId).orElseThrow();
        ReflectionTestUtils.setField(request, "expiresAt", LocalDateTime.now().minusMinutes(1));
        lessonExchangeRequestRepository.save(request);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("content", "만료된 요청 제안"))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("같은 요청에 활성 제안을 이미 작성했으면 -> 409")
    void createProposal_duplicateActiveProposal_returns409() {
        LocalDate requestDate = LocalDate.now().plusDays(11);
        LocalDate proposalDate = LocalDate.now().plusDays(12);
        Long requestId = createApprovedFullRequest(requestDate);

        Long proposerSubjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        registerLesson(proposerSubjectId, TEACHER2_ID, proposalDate, "09:00:00", "10:00:00", 1);

        Long proposalId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", proposalDate.toString(),
                "content", "첫 제안"
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
        proposalIds.add(proposalId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", proposalDate.toString(),
                "content", "중복 제안"
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("존재하지 않는 요청에 제안 생성 -> 404")
    void createProposal_requestNotFound_returns404() {
        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("content", "없는 요청 제안"))
            .post("/{requestId}/proposals", 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("제안 내용이 빈 문자열이면 -> 400")
    void createProposal_blankContent_returns400() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(12));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("content", ""))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("제안 내용이 누락되면 -> 400")
    void createProposal_missingContent_returns400() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(13));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", LocalDate.now().plusDays(14).toString()
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("대체형 제안에 교시 범위를 넣으면 -> 400")
    void createProposal_substitutionWithPeriods_returns400() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(13));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "startPeriod", 1,
                "endPeriod", 2,
                "content", "유효하지 않은 대체형 제안"
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("요청과 같은 시간대 수업을 교환형으로 제안하면 -> 400")
    void createProposal_overlappingWithRequest_returns400() {
        LocalDate requestDate = LocalDate.now().plusDays(14);
        Long requestId = createApprovedPartialRequest(requestDate, 1, 2);

        Long proposerSubjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        registerLesson(proposerSubjectId, TEACHER2_ID, requestDate, "09:00:00", "09:50:00", 1);
        registerLesson(proposerSubjectId, TEACHER2_ID, requestDate, "10:00:00", "10:50:00", 2);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", requestDate.toString(),
                "startPeriod", 1,
                "endPeriod", 2,
                "content", "같은 시간대 제안"
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("요청 범위와 일부만 겹치는 PARTIAL 제안도 불가 -> 400")
    void createProposal_partiallyOverlappingWithRequest_returns400() {
        LocalDate requestDate = LocalDate.now().plusDays(15);
        Long requestId = createApprovedPartialRequest(requestDate, 1, 2);

        Long proposerSubjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        registerLesson(proposerSubjectId, TEACHER2_ID, requestDate, "10:00:00", "10:50:00", 2);
        registerLesson(proposerSubjectId, TEACHER2_ID, requestDate, "11:00:00", "11:50:00", 3);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", requestDate.toString(),
                "startPeriod", 2,
                "endPeriod", 3,
                "content", "일부만 겹치는 제안"
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("교환형 제안에서 startPeriod 만 입력하면 -> 400")
    void createProposal_withOnlyStartPeriod_returns400() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(16));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", LocalDate.now().plusDays(17).toString(),
                "startPeriod", 1,
                "content", "종료 교시 누락"
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("교환형 제안에서 endPeriod 만 입력하면 -> 400")
    void createProposal_withOnlyEndPeriod_returns400() {
        Long requestId = createApprovedFullRequest(LocalDate.now().plusDays(18));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", LocalDate.now().plusDays(19).toString(),
                "endPeriod", 2,
                "content", "시작 교시 누락"
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("교환형 제안 조건에 맞는 수업이 없으면 -> 400")
    void createProposal_withoutProposalLessons_returns400() {
        LocalDate requestDate = LocalDate.now().plusDays(15);
        Long requestId = createApprovedPartialRequest(requestDate, 1, 2);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", LocalDate.now().plusDays(16).toString(),
                "startPeriod", 1,
                "endPeriod", 2,
                "content", "해당 날짜 수업 없음"
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("교환형 전체 제안에 여러 반 수업이 섞이면 -> 409")
    void createProposal_fullScopeAcrossMultipleClassrooms_returns409() {
        LocalDate requestDate = LocalDate.now().plusDays(16);
        LocalDate proposalDate = LocalDate.now().plusDays(17);
        Long requestId = createApprovedFullRequest(requestDate);

        Long subjectId1 = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        Long subjectId2 = registerSubject(2L, TEACHER2_ID);
        registerLesson(subjectId1, TEACHER2_ID, proposalDate, "09:00:00", "09:50:00", 1);
        registerLesson(subjectId2, TEACHER2_ID, proposalDate, "10:00:00", "10:50:00", 2);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", proposalDate.toString(),
                "content", "다른 반 수업이 섞인 전체 교환 제안"
            ))
            .post("/{requestId}/proposals", requestId)
            .then()
            .statusCode(409);
    }

    private Long createApprovedFullRequest(LocalDate lessonDate) {
        Long requestId = createPendingFullRequest(VOLUNTEER_USERNAME, TEACHER_ID, lessonDate);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"));

        return requestId;
    }

    private Long createApprovedPartialRequest(LocalDate lessonDate, int startPeriod, int endPeriod) {
        Long requestId = createPendingPartialRequest(
            VOLUNTEER_USERNAME, TEACHER_ID, lessonDate, startPeriod, endPeriod
        );

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"));

        return requestId;
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
            "해당 날짜 수업 교환을 요청합니다.",
            null,
            null,
            lessonDate.minusDays(3).atTime(23, 0)
        );
        requestIds.add(requestId);
        return requestId;
    }

    private Long createPendingPartialRequest(
        String username,
        Long teacherId,
        LocalDate lessonDate,
        int startPeriod,
        int endPeriod
    ) {
        String authHeader = VOLUNTEER_USERNAME.equals(username)
            ? getAuthHeader(volunteerToken)
            : getAuthHeader(volunteer2Token);

        Long subjectId = registerSubject(CLASSROOM_ID, teacherId);
        if (startPeriod <= 1 && endPeriod >= 1) {
            registerLesson(subjectId, teacherId, lessonDate, "09:00:00", "09:50:00", 1);
        }
        if (startPeriod <= 2 && endPeriod >= 2) {
            registerLesson(subjectId, teacherId, lessonDate, "10:00:00", "10:50:00", 2);
        }
        if (startPeriod <= 3 && endPeriod >= 3) {
            registerLesson(subjectId, teacherId, lessonDate, "11:00:00", "11:50:00", 3);
        }

        Long requestId = createLessonExchangeRequest(
            authHeader,
            lessonDate,
            "부분 수업 교환 요청",
            "일부 교시만 교환을 요청합니다.",
            startPeriod,
            endPeriod,
            lessonDate.minusDays(3).atTime(23, 0)
        );
        requestIds.add(requestId);
        return requestId;
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
