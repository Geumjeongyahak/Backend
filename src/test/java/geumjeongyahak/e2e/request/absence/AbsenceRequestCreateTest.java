package geumjeongyahak.e2e.request.absence;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.request.repository.AbsenceRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;

@Tag("absence-request")
@DisplayName("E2E: 결석 요청 생성 테스트")
class AbsenceRequestCreateTest extends RequestBaseTest {

    private static final long ADMIN_ID = 1L;

    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;

    private Long createdSubjectId;
    private Long createdLessonId;
    private Long createdRequestId;
    private Long createdRequestId2;

    @AfterEach
    void cleanup() {
        if (createdRequestId2 != null) {
            absenceRequestRepository.deleteById(createdRequestId2);
            createdRequestId2 = null;
        }
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

        createdRequestId = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", createdLessonId, "title", "개인 사정 결석", "reason", "개인 사정"))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("lessonId", equalTo(createdLessonId.intValue()))
            .body("title", equalTo("개인 사정 결석"))
            .body("reason", equalTo("개인 사정"))
            .body("expiresAt", equalTo(lessonHelper.getLessonDate(
                getAuthHeader(adminToken), createdLessonId
            ) + "T00:00:00"))
            .body("status", equalTo("PENDING"))
            .body("requestedByName", equalTo("홍길동"))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Test
    @DisplayName("관리자가 본인 담당 수업으로 결석 요청 생성 → 201")
    void createAbsenceRequest_asAdminTeacher_returns201() {
        createdSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, ADMIN_ID);
        createdLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), createdSubjectId, ADMIN_ID);

        createdRequestId = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", createdLessonId, "title", "관리자 결석", "reason", "관리자 결석 사유"))
            .post()
            .then()
            .statusCode(201)
            .body("title", equalTo("관리자 결석"))
            .body("status", equalTo("PENDING"))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Test
    @DisplayName("관리자가 타인 담당 수업으로 결석 요청 생성 → 403")
    void createAbsenceRequest_asAdminForOthersLesson_returns403() {
        createdSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        createdLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), createdSubjectId, TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", createdLessonId, "title", "관리자 대리 결석", "reason", "관리자 대리 요청"))
            .post()
            .then()
            .statusCode(403);
    }

    // ── 인증 오류 ─────────────────────────────────────────

    @Test
    @DisplayName("인증 없이 결석 요청 생성 → 401")
    void createAbsenceRequest_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/absence-requests")
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", 1, "title", "결석 요청", "reason", "사유"))
            .post()
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("게스트가 결석 요청 생성 → 403")
    void createAbsenceRequest_asGuest_returns403() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", 1, "title", "결석 요청", "reason", "사유"))
            .post()
            .then()
            .statusCode(403);
    }

    // ── 도메인 오류 ───────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 수업 ID로 결석 요청 → 404")
    void createAbsenceRequest_nonExistentLesson_returns404() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", 99999L, "title", "결석 요청", "reason", "사유"))
            .post()
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("이미 만료 시각이 지난 수업으로 결석 요청 생성 → 400")
    void createAbsenceRequest_expiredLesson_returns400() {
        createdSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        createdLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken),
            createdSubjectId,
            TEACHER_ID,
            LocalDate.now().toString(),
            "09:00:00",
            "10:00:00",
            1
        );

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", createdLessonId, "title", "만료된 결석", "reason", "만료된 결석 요청"))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("타인 담당 수업으로 결석 요청 생성 → 403")
    void createAbsenceRequest_forOthersLesson_returns403() {
        createdSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        createdLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), createdSubjectId, TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", createdLessonId, "title", "타인 수업 결석", "reason", "타인 수업 결석 요청"))
            .post()
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("동일 수업에 PENDING 결석 요청이 있으면 중복 생성 → 409")
    void createAbsenceRequest_duplicatePending_returns409() {
        createdSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        createdLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), createdSubjectId, TEACHER_ID);
        createdRequestId = createAbsenceRequest(
            getAuthHeader(volunteerToken), createdLessonId, "기존 결석 요청");

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", createdLessonId, "title", "중복 결석", "reason", "중복 결석 요청"))
            .post()
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("동일 수업에 APPROVED 결석 요청이 있으면 중복 생성 → 409")
    void createAbsenceRequest_duplicateApproved_returns409() {
        createdSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        createdLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), createdSubjectId, TEACHER_ID);
        createdRequestId = createAbsenceRequest(
            getAuthHeader(volunteerToken), createdLessonId, "기존 결석 요청");

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", createdRequestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", createdLessonId, "title", "승인 후 중복 결석", "reason", "승인 후 중복 결석 요청"))
            .post()
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("동일 수업에 REJECTED 결석 요청만 있으면 재요청 → 201")
    void createAbsenceRequest_afterRejected_returns201() {
        createdSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        createdLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), createdSubjectId, TEACHER_ID);
        createdRequestId = createAbsenceRequest(
            getAuthHeader(volunteerToken), createdLessonId, "기존 결석 요청");

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "반려 사유"))
            .patch("/{id}/reject", createdRequestId)
            .then()
            .statusCode(200);

        createdRequestId2 = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", createdLessonId, "title", "반려 후 재요청", "reason", "반려 후 재요청"))
            .post()
            .then()
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Test
    @DisplayName("동일 수업에 CANCELLED 결석 요청만 있으면 재요청 → 201")
    void createAbsenceRequest_afterCancelled_returns201() {
        createdSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        createdLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), createdSubjectId, TEACHER_ID);
        createdRequestId = createAbsenceRequest(
            getAuthHeader(volunteerToken), createdLessonId, "기존 결석 요청");

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{id}", createdRequestId)
            .then()
            .statusCode(204);

        createdRequestId2 = given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", createdLessonId, "title", "취소 후 재요청", "reason", "취소 후 재요청"))
            .post()
            .then()
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    // ── 유효성 오류 ───────────────────────────────────────

    @Test
    @DisplayName("reason 이 빈 문자열 → 400")
    void createAbsenceRequest_blankReason_returns400() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", 1, "title", "결석 요청", "reason", ""))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("title 이 빈 문자열 → 400")
    void createAbsenceRequest_blankTitle_returns400() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("lessonId", 1, "title", "", "reason", "사유"))
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
            .body(Map.of("title", "결석 요청", "reason", "사유"))
            .post()
            .then()
            .statusCode(400);
    }
}
