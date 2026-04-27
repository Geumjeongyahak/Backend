package geumjeongyahak.e2e.request.lessonexchange;

import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag("lesson-exchange-request")
@DisplayName("E2E: 수업 교환 요청 취소 테스트")
class LessonExchangeRequestCancelTest extends RequestBaseTest {

    @Autowired
    private LessonExchangeRequestRepository lessonExchangeRequestRepository;

    private final List<Long> subjectIds = new ArrayList<>();
    private final List<Long> lessonIds = new ArrayList<>();
    private final List<Long> requestIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        requestIds.forEach(requestId -> {
            if (lessonExchangeRequestRepository.existsById(requestId)) {
                lessonExchangeRequestRepository.deleteById(requestId);
            }
        });
        lessonIds.forEach(lessonId -> lessonHelper.deleteLesson(getAuthHeader(adminToken), lessonId));
        subjectIds.forEach(subjectId -> lessonHelper.deleteSubject(getAuthHeader(adminToken), subjectId));
    }

    @Test
    @DisplayName("요청자가 PENDING 요청 취소 -> 200, CANCELLED")
    void cancelPendingRequest_asRequester_returns200() {
        LocalDate lessonDate = LocalDate.now().plusDays(6);
        createFullDayLessons(TEACHER_ID, lessonDate);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "취소 전 요청",
            "요청자 본인이 취소합니다.",
            null,
            null,
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{id}/cancel", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("CANCELLED"))
            .body("cancelledAt", notNullValue());
    }

    @Test
    @DisplayName("이미 취소된 요청을 다시 취소 -> 409")
    void cancelAlreadyCancelledRequest_returns409() {
        LocalDate lessonDate = LocalDate.now().plusDays(7);
        createFullDayLessons(TEACHER_ID, lessonDate);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "이미 취소될 요청",
            "한 번 취소한 뒤 다시 취소 시도",
            null,
            null,
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{id}/cancel", requestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{id}/cancel", requestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("취소 후 상세 조회 시 CANCELLED 와 cancelledAt 이 유지된다")
    void getDetail_afterCancel_containsCancelledInfo() {
        LocalDate lessonDate = LocalDate.now().plusDays(8);
        createFullDayLessons(TEACHER_ID, lessonDate);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "상세 조회용 요청",
            "취소 후 상세 조회 확인",
            null,
            null,
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{id}/cancel", requestId)
            .then()
            .statusCode(200);

        String cancelledAt = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{id}", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("CANCELLED"))
            .body("cancelledAt", notNullValue())
            .extract()
            .jsonPath()
            .getString("cancelledAt");

        assertThat(cancelledAt).isNotBlank();
    }

    @Test
    @DisplayName("취소 후 같은 날짜와 범위로 새 요청 생성 가능 -> 201")
    void createNewRequest_afterCancel_returns201() {
        LocalDate lessonDate = LocalDate.now().plusDays(9);
        createFullDayLessons(TEACHER_ID, lessonDate);

        Long cancelledRequestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "취소될 요청",
            "취소 후 재요청 테스트",
            1,
            1,
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(cancelledRequestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{id}/cancel", cancelledRequestId)
            .then()
            .statusCode(200);

        Long newRequestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "재요청",
            "취소 이후 같은 범위 재요청",
            1,
            1,
            lessonDate.minusDays(3).atTime(21, 0)
        );
        requestIds.add(newRequestId);
    }

    @Test
    @DisplayName("다른 봉사자가 요청 취소 시도 -> 403")
    void cancelRequest_asOtherVolunteer_returns403() {
        LocalDate lessonDate = LocalDate.now().plusDays(10);
        createFullDayLessons(TEACHER_ID, lessonDate);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "타인 취소 방지 요청",
            "다른 사용자는 취소할 수 없습니다.",
            null,
            null,
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .patch("/{id}/cancel", requestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("승인된 요청 취소 시도 -> 409")
    void cancelApprovedRequest_returns409() {
        LocalDate lessonDate = LocalDate.now().plusDays(11);
        createFullDayLessons(TEACHER_ID, lessonDate);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "승인될 요청",
            "승인 후에는 취소할 수 없습니다.",
            null,
            null,
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{id}/approve", requestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{id}/cancel", requestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("인증 없이 요청 취소 -> 401")
    void cancelRequest_unauthenticated_returns401() {
        LocalDate lessonDate = LocalDate.now().plusDays(12);
        createFullDayLessons(TEACHER_ID, lessonDate);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "인증 테스트 요청",
            "인증 없이는 취소할 수 없습니다.",
            null,
            null,
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .patch("/{id}/cancel", requestId)
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("존재하지 않는 요청 취소 -> 404")
    void cancelRequest_notFound_returns404() {
        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{id}/cancel", 99999L)
            .then()
            .statusCode(404);
    }

    private void createFullDayLessons(Long teacherId, LocalDate lessonDate) {
        Long subjectId = registerSubject(CLASSROOM_ID, teacherId);
        registerLesson(subjectId, teacherId, lessonDate, "09:00:00", "09:50:00", 1);
        registerLesson(subjectId, teacherId, lessonDate, "10:00:00", "10:50:00", 2);
        registerLesson(subjectId, teacherId, lessonDate, "11:00:00", "11:50:00", 3);
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
