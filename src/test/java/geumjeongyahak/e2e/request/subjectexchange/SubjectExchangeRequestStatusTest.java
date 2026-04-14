package geumjeongyahak.e2e.request.subjectexchange;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.request.repository.SubjectExchangeRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;

/**
 * 과목 교환 요청 승인·반려·조회 E2E 테스트.
 * 과목 교환 승인은 과목 상태를 변경하지 않으므로 side-effect 검증 불필요.
 */
@Tag("subject-exchange-request")
@DisplayName("E2E: 과목 교환 요청 승인·반려·조회 테스트")
class SubjectExchangeRequestStatusTest extends RequestBaseTest {

    @Autowired
    private SubjectExchangeRequestRepository subjectExchangeRequestRepository;

    private Long currentSubjectId;
    private Long currentLessonId;
    private Long otherSubjectId;
    private Long currentRequestId;

    @AfterEach
    void cleanup() {
        if (currentRequestId != null) {
            if (subjectExchangeRequestRepository.existsById(currentRequestId)) {
                subjectExchangeRequestRepository.deleteById(currentRequestId);
            }
            currentRequestId = null;
        }
        if (currentLessonId != null) {
            lessonHelper.deleteLesson(getAuthHeader(adminToken), currentLessonId);
            currentLessonId = null;
        }
        if (currentSubjectId != null) {
            lessonHelper.deleteSubject(getAuthHeader(adminToken), currentSubjectId);
            currentSubjectId = null;
        }
        if (otherSubjectId != null) {
            lessonHelper.deleteSubject(getAuthHeader(adminToken), otherSubjectId);
            otherSubjectId = null;
        }
    }

    private Long setupPendingRequest() {
        currentSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID
        );
        return createSubjectExchangeRequest(
            getAuthHeader(volunteerToken), currentSubjectId, "과목 교환 요청", "조정 필요합니다."
        );
    }

    // ── 승인 (approve) ────────────────────────────────────

    @Test
    @DisplayName("관리자 과목 교환 요청 승인 → 200, APPROVED")
    void approve_asAdmin_returns200() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", TEACHER2_ID))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("approvalAt", notNullValue())
            .body("approvalByName", notNullValue());
    }

    @Test
    @DisplayName("[Side-effect] 승인 후 과목 담당 교사와 이후 수업 담당 교사가 함께 변경된다")
    void approve_updatesSubjectTeacher_andFutureLessons() {
        currentSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        currentLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), currentSubjectId, TEACHER_ID
        );
        currentRequestId = createSubjectExchangeRequest(
            getAuthHeader(volunteerToken), currentSubjectId, "과목 교환 요청", "조정 필요합니다."
        );

        given()
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", TEACHER2_ID))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(200);

        assertThat(lessonHelper.getSubjectTeacherId(getAuthHeader(adminToken), currentSubjectId))
            .isEqualTo(TEACHER2_ID);
        assertThat(lessonHelper.getLessonTeacherName(getAuthHeader(adminToken), currentLessonId))
            .isEqualTo("김철수");
    }

    @Test
    @DisplayName("이미 처리된 과목 교환 요청 재승인 → 409")
    void approve_alreadyProcessed_returns409() {
        currentRequestId = setupPendingRequest();

        given().basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", TEACHER2_ID))
            .patch("/{id}/approve", currentRequestId)
            .then().statusCode(200);

        given().basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", TEACHER2_ID))
            .patch("/{id}/approve", currentRequestId)
            .then().statusCode(409);
    }

    @Test
    @DisplayName("봉사자 승인 시도 → 403")
    void approve_asVolunteer_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", TEACHER2_ID))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 요청 승인 → 404")
    void approve_notFound_returns404() {
        given()
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", TEACHER2_ID))
            .patch("/{id}/approve", 99999L)
            .then()
            .statusCode(404);
    }

    // ── 반려 (reject) ─────────────────────────────────────

    @Test
    @DisplayName("관리자 과목 교환 요청 반려 → 200, REJECTED, note 저장")
    void reject_asAdmin_withNote_returns200() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "현재 과목 유지 필요"))
            .patch("/{id}/reject", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("REJECTED"))
            .body("note", equalTo("현재 과목 유지 필요"));
    }

    @Test
    @DisplayName("note 없이 반려 → 400")
    void reject_withoutNote_returns400() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of())
            .patch("/{id}/reject", currentRequestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("이미 처리된 요청 반려 → 409")
    void reject_alreadyProcessed_returns409() {
        currentRequestId = setupPendingRequest();

        given().basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", TEACHER2_ID))
            .patch("/{id}/approve", currentRequestId)
            .then().statusCode(200);

        given().basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "이미 승인된 건"))
            .patch("/{id}/reject", currentRequestId)
            .then().statusCode(409);
    }

    @Test
    @DisplayName("봉사자 반려 시도 → 403")
    void reject_asVolunteer_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "반려"))
            .patch("/{id}/reject", currentRequestId)
            .then()
            .statusCode(403);
    }

    // ── 조회 ──────────────────────────────────────────────

    @Test
    @DisplayName("관리자는 전체 목록 / 봉사자는 본인 요청만 조회")
    void getList_adminSeesAll_volunteerSeesOnlyOwn() {
        currentSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID
        );
        otherSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER2_ID
        );
        Long req1 = createSubjectExchangeRequest(
            getAuthHeader(volunteerToken), currentSubjectId, "v1 교환", "v1 내용");
        Long req2 = createSubjectExchangeRequest(
            getAuthHeader(volunteer2Token), otherSubjectId, "v2 교환", "v2 내용");

        try {
            List<Long> adminIds = given()
                .basePath("/api/v1/subject-exchange-requests")
                .header(AUTH_HEADER, getAuthHeader(adminToken))
                .get()
                .then().statusCode(200)
                .extract().jsonPath().getList("id", Long.class);
            assertThat(adminIds).contains(req1, req2);

            List<Long> v1Ids = given()
                .basePath("/api/v1/subject-exchange-requests")
                .header(AUTH_HEADER, getAuthHeader(volunteerToken))
                .get()
                .then().statusCode(200)
                .extract().jsonPath().getList("id", Long.class);
            assertThat(v1Ids).contains(req1);
            assertThat(v1Ids).doesNotContain(req2);

        } finally {
            subjectExchangeRequestRepository.deleteById(req1);
            subjectExchangeRequestRepository.deleteById(req2);
        }
    }

    @Test
    @DisplayName("타인의 단건 조회 → 403")
    void getDetail_byNonOwner_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .get("/{id}", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("인증 없이 목록 조회 → 401")
    void getList_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/subject-exchange-requests")
            .get()
            .then()
            .statusCode(401);
    }
}
