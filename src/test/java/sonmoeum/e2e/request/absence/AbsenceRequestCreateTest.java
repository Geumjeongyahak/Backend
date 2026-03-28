package sonmoeum.e2e.request.absence;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import sonmoeum.domain.request.repository.AbsenceRequestRepository;
import sonmoeum.e2e.request.RequestBaseTest;

@Tag("absence-request")
@DisplayName("E2E: 결석 요청 생성 테스트")
class AbsenceRequestCreateTest extends RequestBaseTest {

    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;

    private Long createdSubjectId;
    private Long createdLessonId;
    private Long createdRequestId;

    @AfterEach
    void cleanup() {
        if (createdRequestId != null) {
            absenceRequestRepository.deleteById(createdRequestId);
            createdRequestId = null;
        }
        if (createdLessonId != null) {
            lessonHelper.deleteLesson(getAuthHeader(adminToken), createdLessonId);
            createdLessonId = null;
        }
        if (createdSubjectId != null) {
            lessonHelper.deleteSubject(getAuthHeader(adminToken), createdSubjectId);
            createdSubjectId = null;
        }
    }

    // ── 성공 ──────────────────────────────────────────────

    @Test
    @DisplayName("봉사자가 유효한 수업으로 결석 요청 생성 → 201, 필드 검증")
    void createAbsenceRequest_asVolunteer_returns201() {
        createdSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        createdLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), createdSubjectId, TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", createdLessonId, "reason", "개인 사정"))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("lessonId", equalTo(createdLessonId.intValue()))
            .body("reason", equalTo("개인 사정"))
            .body("status", equalTo("PENDING"))
            .body("requestedByName", equalTo("홍길동"));

        // 생성된 요청 추적 (cleanup)
        createdRequestId = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get()
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getLong("[0].id");
    }

    @Test
    @DisplayName("관리자가 결석 요청 생성 → 201")
    void createAbsenceRequest_asAdmin_returns201() {
        createdSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        createdLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), createdSubjectId, TEACHER_ID);

        createdRequestId = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", createdLessonId, "reason", "관리자 결석 사유"))
            .post()
            .then()
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    // ── 인증 오류 ─────────────────────────────────────────

    @Test
    @DisplayName("인증 없이 결석 요청 생성 → 401")
    void createAbsenceRequest_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/absence-requests")
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", 1, "reason", "사유"))
            .post()
            .then()
            .statusCode(401);
    }

    // ── 도메인 오류 ───────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 수업 ID로 결석 요청 → 404")
    void createAbsenceRequest_nonExistentLesson_returns404() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", 99999L, "reason", "사유"))
            .post()
            .then()
            .statusCode(404);
    }

    // ── 유효성 오류 ───────────────────────────────────────

    @Test
    @DisplayName("reason 이 빈 문자열 → 400")
    void createAbsenceRequest_blankReason_returns400() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", 1, "reason", ""))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("lessonId 가 null → 400")
    void createAbsenceRequest_nullLessonId_returns400() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("reason", "사유"))
            .post()
            .then()
            .statusCode(400);
    }
}
