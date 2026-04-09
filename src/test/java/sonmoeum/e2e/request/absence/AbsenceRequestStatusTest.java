package sonmoeum.e2e.request.absence;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
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

/**
 * 결석 요청 상태 변경(승인·반려·삭제) E2E 테스트.
 *
 * <h3>Side-effect 검증</h3>
 * 결석 요청 승인 시 수업의 teacherAttendance 가 EXCUSED 로 변경되는지 확인한다.
 * 각 테스트는 독립 수업을 생성해 다른 테스트의 수업 상태에 영향을 주지 않는다.
 */
@Tag("absence-request")
@DisplayName("E2E: 결석 요청 승인·반려·삭제 테스트")
class AbsenceRequestStatusTest extends RequestBaseTest {

    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;

    private Long currentSubjectId;
    private Long currentLessonId;
    private Long currentRequestId;

    @AfterEach
    void cleanup() {
        if (currentRequestId != null) {
            if (absenceRequestRepository.existsById(currentRequestId)) {
                absenceRequestRepository.deleteById(currentRequestId);
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

    // ── 준비 헬퍼 ─────────────────────────────────────────

    /** 독립 수업 + 결석 요청을 생성하고 요청 ID를 반환한다. */
    private Long setupPendingRequest(long teacherId) {
        currentSubjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, teacherId);
        currentLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), currentSubjectId, teacherId);
        return createAbsenceRequest(getAuthHeader(volunteerToken), currentLessonId, "결석 사유");
    }

    // ── 승인 (approve) ────────────────────────────────────

    @Test
    @DisplayName("관리자 결석 요청 승인 → 200, APPROVED, approvalAt 설정")
    void approve_asAdmin_returns200WithApproved() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("approvalAt", notNullValue())
            .body("approvalByName", notNullValue());
    }

    @Test
    @DisplayName("[Side-effect] 결석 요청 승인 → 수업 teacherAttendance 가 EXCUSED 로 변경")
    void approve_updatesLessonTeacherAttendance_toExcused() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        // 승인 전: ABSENT (init_data 기본값 아닌 새 수업 기본값)
        String before = lessonHelper.getLessonTeacherAttendance(
            getAuthHeader(adminToken), currentLessonId);
        assertThat(before).isEqualTo("ABSENT");

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(200);

        // 승인 후: EXCUSED
        String after = lessonHelper.getLessonTeacherAttendance(
            getAuthHeader(adminToken), currentLessonId);
        assertThat(after).isEqualTo("EXCUSED");
    }

    @Test
    @DisplayName("이미 승인된 요청 재승인 → 409 (이미 처리된 요청)")
    void approve_alreadyApproved_returns409() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given().basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", currentRequestId)
            .then().statusCode(200);

        given().basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", currentRequestId)
            .then().statusCode(409);
    }

    @Test
    @DisplayName("반려된 요청 승인 시도 → 409")
    void approve_afterReject_returns409() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given().basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "반려 사유"))
            .patch("/{id}/reject", currentRequestId)
            .then().statusCode(200);

        given().basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", currentRequestId)
            .then().statusCode(409);
    }

    @Test
    @DisplayName("봉사자가 승인 시도 → 403")
    void approve_asVolunteer_returns403() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 요청 승인 → 404")
    void approve_notFound_returns404() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("인증 없이 승인 시도 → 401")
    void approve_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/absence-requests")
            .patch("/{id}/approve", 1L)
            .then()
            .statusCode(401);
    }

    // ── 반려 (reject) ─────────────────────────────────────

    @Test
    @DisplayName("관리자 결석 요청 반려 → 200, REJECTED, note 저장")
    void reject_asAdmin_withNote_returns200() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "반려 사유입니다."))
            .patch("/{id}/reject", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("REJECTED"))
            .body("note", equalTo("반려 사유입니다."))
            .body("approvalAt", notNullValue());
    }

    @Test
    @DisplayName("note 없이 반려 요청 → 400 (validation)")
    void reject_withoutNote_returns400() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of())
            .patch("/{id}/reject", currentRequestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("빈 note 로 반려 요청 → 400 (blank validation)")
    void reject_withBlankNote_returns400() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", ""))
            .patch("/{id}/reject", currentRequestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("이미 처리된 요청 반려 → 409")
    void reject_alreadyProcessed_returns409() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given().basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", currentRequestId)
            .then().statusCode(200);

        given().basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "뒤늦은 반려"))
            .patch("/{id}/reject", currentRequestId)
            .then().statusCode(409);
    }

    @Test
    @DisplayName("봉사자가 반려 시도 → 403")
    void reject_asVolunteer_returns403() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "반려"))
            .patch("/{id}/reject", currentRequestId)
            .then()
            .statusCode(403);
    }

    // ── 삭제 (delete) ─────────────────────────────────────

    @Test
    @DisplayName("관리자가 임의 요청 삭제 → 204")
    void delete_asAdmin_returns204() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .delete("/{id}", currentRequestId)
            .then()
            .statusCode(204);

        // 삭제 후 cleanup 에서 deleteById 가 없는 항목을 처리하므로 null 처리
        currentRequestId = null;
    }

    @Test
    @DisplayName("요청 소유자(봉사자1)가 본인 요청 삭제 → 204")
    void delete_asOwner_returns204() {
        currentRequestId = setupPendingRequest(TEACHER_ID);
        Long requestId = currentRequestId;

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{id}", requestId)
            .then()
            .statusCode(204);

        currentRequestId = null;
    }

    @Test
    @DisplayName("타인(봉사자2)이 봉사자1 요청 삭제 → 403")
    void delete_asOtherVolunteer_returns403() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .delete("/{id}", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("이미 처리된 요청 삭제 → 409")
    void delete_processedRequest_returns409() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .delete("/{id}", currentRequestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("존재하지 않는 요청 삭제 → 404")
    void delete_notFound_returns404() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .delete("/{id}", 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("인증 없이 삭제 → 401")
    void delete_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/absence-requests")
            .delete("/{id}", 1L)
            .then()
            .statusCode(401);
    }
}
