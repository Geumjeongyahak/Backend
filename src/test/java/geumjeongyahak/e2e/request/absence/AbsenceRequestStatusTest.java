package geumjeongyahak.e2e.request.absence;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.request.repository.AbsenceRequestRepository;
import geumjeongyahak.domain.request.service.AbsenceRequestService;
import geumjeongyahak.e2e.request.RequestBaseTest;

/**
 * 결석 요청 상태 변경(승인·반려·삭제) E2E 테스트.
 *
 */
@Tag("absence-request")
@DisplayName("E2E: 결석 요청 승인·반려·삭제 테스트")
class AbsenceRequestStatusTest extends RequestBaseTest {

    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;

    @Autowired
    private AbsenceRequestService absenceRequestService;

    @Autowired
    private DailyScheduleRepository dailyScheduleRepository;

    @Autowired
    private DailyTeacherAttendanceRepository dailyTeacherAttendanceRepository;

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
    @DisplayName("결석 요청 승인 시 DailySchedule 교사 출석은 공결로 반영된다")
    void approve_updatesDailyScheduleTeacherAttendanceToExcused() {
        currentRequestId = setupPendingRequest(TEACHER_ID);
        LocalDate lessonDate = LocalDate.parse(lessonHelper.getLessonDate(getAuthHeader(adminToken), currentLessonId));

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(200);

        var dailySchedule = dailyScheduleRepository
            .findByClassroomIdAndLessonDateAndIsDeletedFalse(CLASSROOM_ID, lessonDate)
            .orElseThrow();
        Long dailyScheduleId = dailySchedule.getId();

        assertThat(dailyTeacherAttendanceRepository
            .findByDailyScheduleIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow()
            .getStatus()
        ).isEqualTo(DailyTeacherAttendanceStatus.EXCUSED);
        assertThat(dailySchedule.isAbsent()).isTrue();
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
    @DisplayName("취소된 요청 승인 시도 → 409")
    void approve_afterCancel_returns409() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given().basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{id}", currentRequestId)
            .then().statusCode(204);

        given().basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", currentRequestId)
            .then().statusCode(409);
    }

    @Test
    @DisplayName("만료된 요청 승인 시도 → 409")
    void approve_afterExpire_returns409() {
        currentRequestId = setupPendingRequest(TEACHER_ID);
        setRequestExpiresAt(currentRequestId, LocalDateTime.now().minusMinutes(1));
        absenceRequestService.expireExpiredAbsenceRequests();

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
    @DisplayName("취소된 요청 반려 시도 → 409")
    void reject_afterCancel_returns409() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given().basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{id}", currentRequestId)
            .then().statusCode(204);

        given().basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "취소 후 반려"))
            .patch("/{id}/reject", currentRequestId)
            .then().statusCode(409);
    }

    @Test
    @DisplayName("만료된 요청 반려 시도 → 409")
    void reject_afterExpire_returns409() {
        currentRequestId = setupPendingRequest(TEACHER_ID);
        setRequestExpiresAt(currentRequestId, LocalDateTime.now().minusMinutes(1));
        absenceRequestService.expireExpiredAbsenceRequests();

        given().basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "만료 후 반려"))
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

    // ── 취소 (delete) ─────────────────────────────────────

    @Test
    @DisplayName("관리자가 임의 요청 취소 → 403")
    void delete_asAdmin_returns403() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .delete("/{id}", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("요청 소유자(봉사자1)가 본인 요청 취소 → 204, CANCELLED")
    void delete_asOwner_returns204() {
        currentRequestId = setupPendingRequest(TEACHER_ID);
        Long requestId = currentRequestId;

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{id}", requestId)
            .then()
            .statusCode(204);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{id}", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("CANCELLED"));
    }

    @Test
    @DisplayName("타인(봉사자2)이 봉사자1 요청 취소 → 403")
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
    @DisplayName("이미 처리된 요청 취소 → 409")
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
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{id}", currentRequestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("이미 취소된 요청 재취소 → 409")
    void delete_alreadyCancelled_returns409() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{id}", currentRequestId)
            .then()
            .statusCode(204);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{id}", currentRequestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("만료된 요청 취소 시도 → 409")
    void delete_afterExpire_returns409() {
        currentRequestId = setupPendingRequest(TEACHER_ID);
        setRequestExpiresAt(currentRequestId, LocalDateTime.now().minusMinutes(1));
        absenceRequestService.expireExpiredAbsenceRequests();

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{id}", currentRequestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("자동 만료 처리 시 만료 시각이 지난 PENDING 요청은 EXPIRED 로 변경")
    void expireExpiredAbsenceRequests_changesExpiredPendingRequestStatus() {
        currentRequestId = setupPendingRequest(TEACHER_ID);
        setRequestExpiresAt(currentRequestId, LocalDateTime.now().minusMinutes(1));

        int expiredCount = absenceRequestService.expireExpiredAbsenceRequests();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(absenceRequestRepository.findById(currentRequestId).orElseThrow().getStatus())
            .isEqualTo(RequestStatus.EXPIRED);
    }

    @Test
    @DisplayName("자동 만료 처리 시 아직 만료되지 않은 요청은 PENDING 유지")
    void expireExpiredAbsenceRequests_keepsNotExpiredRequestUntouched() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        int expiredCount = absenceRequestService.expireExpiredAbsenceRequests();

        assertThat(expiredCount).isZero();
        assertThat(absenceRequestRepository.findById(currentRequestId).orElseThrow().getStatus())
            .isEqualTo(RequestStatus.PENDING);
    }

    @Test
    @DisplayName("자동 만료 처리 시 이미 처리된 요청은 그대로 유지")
    void expireExpiredAbsenceRequests_keepsAlreadyProcessedRequestUntouched() {
        currentRequestId = setupPendingRequest(TEACHER_ID);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", currentRequestId)
            .then()
            .statusCode(200);
        setRequestExpiresAt(currentRequestId, LocalDateTime.now().minusMinutes(1));

        int expiredCount = absenceRequestService.expireExpiredAbsenceRequests();

        assertThat(expiredCount).isZero();
        assertThat(absenceRequestRepository.findById(currentRequestId).orElseThrow().getStatus())
            .isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    @DisplayName("존재하지 않는 요청 취소 → 404")
    void delete_notFound_returns404() {
        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .delete("/{id}", 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("인증 없이 취소 → 401")
    void delete_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/absence-requests")
            .delete("/{id}", 1L)
            .then()
            .statusCode(401);
    }

    private void setRequestExpiresAt(Long requestId, LocalDateTime expiresAt) {
        var request = absenceRequestRepository.findById(requestId).orElseThrow();
        ReflectionTestUtils.setField(request, "expiresAt", expiresAt);
        absenceRequestRepository.save(request);
    }
}
