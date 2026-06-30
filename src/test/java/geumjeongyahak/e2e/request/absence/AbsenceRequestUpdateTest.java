package geumjeongyahak.e2e.request.absence;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import geumjeongyahak.domain.request.repository.AbsenceRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("absence-request")
@DisplayName("E2E: 결석 요청 수정 테스트")
class AbsenceRequestUpdateTest extends RequestBaseTest {

    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;

    private Long subjectId;
    private Long lessonId;
    private Long requestId;

    @AfterEach
    void cleanup() {
        if (requestId != null && absenceRequestRepository.existsById(requestId)) {
            absenceRequestRepository.deleteById(requestId);
            requestId = null;
        }
        if (lessonId != null) {
            lessonHelper.deleteLesson(getAuthHeader(adminToken), lessonId);
            lessonId = null;
        }
        if (subjectId != null) {
            lessonHelper.deleteSubject(getAuthHeader(adminToken), subjectId);
            subjectId = null;
        }
    }

    @Test
    @DisplayName("요청자가 PENDING 결석 요청 수정 → 200, 제목과 사유 변경")
    void updatePendingRequest_asOwner_returns200() {
        requestId = setupPendingRequest();

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("title", "수정된 결석 요청", "reason", "수정된 결석 사유"))
            .patch("/{id}", requestId)
            .then()
            .statusCode(200)
            .body("title", equalTo("수정된 결석 요청"))
            .body("reason", equalTo("수정된 결석 사유"))
            .body("status", equalTo("PENDING"));
    }

    @Test
    @DisplayName("타인이 결석 요청 수정 시도 → 403")
    void updateRequest_asOtherVolunteer_returns403() {
        requestId = setupPendingRequest();

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("title", "타인 수정 시도", "reason", "수정 불가"))
            .patch("/{id}", requestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("승인된 결석 요청 수정 시도 → 409")
    void updateApprovedRequest_returns409() {
        requestId = setupPendingRequest();

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("title", "승인 후 수정", "reason", "수정 불가"))
            .patch("/{id}", requestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("title 이 빈 문자열이면 → 400")
    void updateRequest_blankTitle_returns400() {
        requestId = setupPendingRequest();

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("title", "", "reason", "수정 사유"))
            .patch("/{id}", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("reason 이 빈 문자열이면 → 400")
    void updateRequest_blankReason_returns400() {
        requestId = setupPendingRequest();

        given()
            .basePath("/api/v1/absence-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("title", "수정 제목", "reason", ""))
            .patch("/{id}", requestId)
            .then()
            .statusCode(400);
    }

    private Long setupPendingRequest() {
        subjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        lessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), subjectId, TEACHER_ID);
        return createAbsenceRequest(
            getAuthHeader(volunteerToken), lessonId, "기존 결석 요청", "기존 결석 사유");
    }
}
