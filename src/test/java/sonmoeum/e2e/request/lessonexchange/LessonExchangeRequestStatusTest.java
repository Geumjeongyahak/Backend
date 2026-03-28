package sonmoeum.e2e.request.lessonexchange;

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
import sonmoeum.domain.request.repository.LessonExchangeRequestRepository;
import sonmoeum.e2e.request.RequestBaseTest;

/**
 * 수업 교환 요청 승인·반려·조회 E2E 테스트.
 *
 * <h3>Side-effect 검증</h3>
 * 수업 교환 요청 승인 시 수업의 teacherName 이 exchangeWithUserId 사용자 이름으로 변경된다.
 * - teacher01(홍길동, id=2) → teacher02(김철수, id=3) 로 교환
 */
@Tag("lesson-exchange-request")
@DisplayName("E2E: 수업 교환 요청 승인·반려·조회 테스트")
class LessonExchangeRequestStatusTest extends RequestBaseTest {

    @Autowired
    private LessonExchangeRequestRepository lessonExchangeRequestRepository;

    private Long currentSubjectId;
    private Long currentLessonId;
    private Long currentRequestId;

    @AfterEach
    void cleanup() {
        if (currentRequestId != null) {
            if (lessonExchangeRequestRepository.existsById(currentRequestId)) {
                lessonExchangeRequestRepository.deleteById(currentRequestId);
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
    }

    private Long setupPendingRequest() {
        currentSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        currentLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), currentSubjectId, TEACHER_ID);
        return createLessonExchangeRequest(
            getAuthHeader(volunteerToken), currentLessonId, "교환 요청", "수업 교환 부탁드립니다.");
    }

    // ── 승인 (approve) ────────────────────────────────────

    @Test
    @DisplayName("관리자 수업 교환 요청 승인 → 200, APPROVED")
    void approve_asAdmin_returns200() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", TEACHER2_ID))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("approvalAt", notNullValue());
    }

    @Test
    @DisplayName("[Side-effect] 승인 후 수업 담당 교사가 teacher02(김철수)로 변경")
    void approve_updatesLessonTeacher_toExchangeTarget() {
        currentRequestId = setupPendingRequest();

        String teacherBefore = lessonHelper.getLessonTeacherName(
            getAuthHeader(adminToken), currentLessonId);
        assertThat(teacherBefore).isEqualTo("홍길동");  // teacher01

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", TEACHER2_ID))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(200);

        String teacherAfter = lessonHelper.getLessonTeacherName(
            getAuthHeader(adminToken), currentLessonId);
        assertThat(teacherAfter).isEqualTo("김철수");   // teacher02
    }

    @Test
    @DisplayName("교환 대상 사용자 ID 가 존재하지 않으면 → 404")
    void approve_withNonExistentExchangeUser_returns404() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", 99999L))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("이미 처리된 수업 교환 요청 재승인 → 409")
    void approve_alreadyProcessed_returns409() {
        currentRequestId = setupPendingRequest();

        given().basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", TEACHER2_ID))
            .patch("/{id}/approve", currentRequestId)
            .then().statusCode(200);

        given().basePath("/api/v1/lesson-exchange-requests")
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
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", TEACHER2_ID))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(403);
    }

    // ── 반려 (reject) ─────────────────────────────────────

    @Test
    @DisplayName("관리자 수업 교환 요청 반려 → 200, REJECTED, note 저장")
    void reject_asAdmin_withNote_returns200() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "스케줄 충돌로 반려합니다."))
            .patch("/{id}/reject", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("REJECTED"))
            .body("note", equalTo("스케줄 충돌로 반려합니다."));
    }

    @Test
    @DisplayName("note 없이 반려 → 400")
    void reject_withoutNote_returns400() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/lesson-exchange-requests")
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

        given().basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("exchangeWithUserId", TEACHER2_ID))
            .patch("/{id}/approve", currentRequestId)
            .then().statusCode(200);

        given().basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "뒤늦은 반려"))
            .patch("/{id}/reject", currentRequestId)
            .then().statusCode(409);
    }

    @Test
    @DisplayName("봉사자 반려 시도 → 403")
    void reject_asVolunteer_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "반려"))
            .patch("/{id}/reject", currentRequestId)
            .then()
            .statusCode(403);
    }

    // ── 조회 (권한별 분리) ────────────────────────────────

    @Test
    @DisplayName("관리자는 전체 목록 조회 / 봉사자는 본인 요청만 조회")
    void getList_adminSeesAll_volunteerSeesOnlyOwn() {
        // volunteer1 요청 생성
        Long subjectId1 = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        currentLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), subjectId1, TEACHER_ID);
        currentRequestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken), currentLessonId, "v1 교환", "내용");

        // volunteer2 요청 생성 (독립 수업 필요 – lesson은 하나만 추적하므로 별도 처리)
        Long subjectId2 = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER2_ID);
        Long lesson2Id = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), subjectId2, TEACHER2_ID);
        Long request2Id = createLessonExchangeRequest(
            getAuthHeader(volunteer2Token), lesson2Id, "v2 교환", "내용");

        try {
            // 관리자: 둘 다 조회됨
            List<Long> adminView = given()
                .basePath("/api/v1/lesson-exchange-requests")
                .header(AUTH_HEADER, getAuthHeader(adminToken))
                .get()
                .then().statusCode(200)
                .extract().jsonPath().getList("id", Long.class);
            assertThat(adminView).contains(currentRequestId, request2Id);

            // volunteer1: 본인 요청만
            List<Long> v1View = given()
                .basePath("/api/v1/lesson-exchange-requests")
                .header(AUTH_HEADER, getAuthHeader(volunteerToken))
                .get()
                .then().statusCode(200)
                .extract().jsonPath().getList("id", Long.class);
            assertThat(v1View).contains(currentRequestId);
            assertThat(v1View).doesNotContain(request2Id);

        } finally {
            lessonExchangeRequestRepository.deleteById(request2Id);
            lessonHelper.deleteLesson(getAuthHeader(adminToken), lesson2Id);
            lessonHelper.deleteSubject(getAuthHeader(adminToken), subjectId2);
        }
    }

    @Test
    @DisplayName("타인의 단건 조회 시도 → 403")
    void getDetail_nonOwner_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .get("/{id}", currentRequestId)
            .then()
            .statusCode(403);
    }
}
