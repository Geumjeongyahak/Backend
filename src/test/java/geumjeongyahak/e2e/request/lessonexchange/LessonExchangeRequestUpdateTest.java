package geumjeongyahak.e2e.request.lessonexchange;

import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@Tag("lesson-exchange-request")
@DisplayName("E2E: 수업 교환 요청 수정 테스트")
class LessonExchangeRequestUpdateTest extends RequestBaseTest {

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
    @DisplayName("요청자가 PENDING 요청 수정 -> 200, 하루 단위 필드 유지")
    void updatePendingRequest_asRequester_returns200() {
        LocalDate lessonDate = LocalDate.now().plusDays(6);
        createLessons(TEACHER_ID, lessonDate, 1, 2);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "기존 요청",
            "기존 내용",
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(buildLessonExchangeRequestBody(
                lessonDate,
                "수정된 요청",
                "수정된 내용",
                lessonDate.minusDays(3).atTime(21, 0)
            ))
            .patch("/{id}", requestId)
            .then()
            .statusCode(200)
            .body("title", equalTo("수정된 요청"))
            .body("content", equalTo("수정된 내용"))
            .body("$", not(hasKey("scope")))
            .body("$", not(hasKey("startPeriod")))
            .body("$", not(hasKey("endPeriod")));
    }

    @Test
    @DisplayName("자기 자신과 같은 날짜로 수정해도 중복으로 보지 않는다 -> 200")
    void updateRequest_sameDateAsSelf_returns200() {
        LocalDate lessonDate = LocalDate.now().plusDays(10);
        createLessons(TEACHER_ID, lessonDate, 1);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "기존 요청",
            "내용",
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(buildLessonExchangeRequestBody(
                lessonDate,
                "제목만 수정",
                "같은 날짜 유지",
                lessonDate.minusDays(3).atTime(21, 0)
            ))
            .patch("/{id}", requestId)
            .then()
            .statusCode(200)
            .body("title", equalTo("제목만 수정"));
    }

    @Test
    @DisplayName("수정 후 다른 활성 요청과 같은 날짜가 되면 -> 409")
    void updateRequest_conflictsWithAnotherActiveRequestOnDate_returns409() {
        LocalDate originalDate = LocalDate.now().plusDays(11);
        LocalDate conflictingDate = LocalDate.now().plusDays(12);
        createLessons(TEACHER_ID, originalDate, 1);
        createLessons(TEACHER_ID, conflictingDate, 1);

        Long firstRequestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            originalDate,
            "첫 번째 요청",
            "원본 날짜 요청",
            originalDate.minusDays(3).atTime(22, 0)
        );
        Long secondRequestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            conflictingDate,
            "두 번째 요청",
            "충돌 날짜 요청",
            conflictingDate.minusDays(3).atTime(21, 30)
        );
        requestIds.add(firstRequestId);
        requestIds.add(secondRequestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(buildLessonExchangeRequestBody(
                conflictingDate,
                "충돌하는 날짜로 수정",
                "이미 요청이 있는 날짜로 변경",
                conflictingDate.minusDays(3).atTime(21, 0)
            ))
            .patch("/{id}", firstRequestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("존재하지 않는 DailySchedule로 수정하면 -> 404")
    void updateRequest_toMissingDailySchedule_returns404() {
        LocalDate lessonDate = LocalDate.now().plusDays(13);
        LocalDate emptyDate = LocalDate.now().plusDays(14);
        createLessons(TEACHER_ID, lessonDate, 1);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "원본 요청",
            "수업이 있는 날짜",
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "dailyScheduleId", 99999L,
                "title", "수업 없는 날짜로 수정",
                "content", "수업이 없는 날짜",
                "expiresAt", emptyDate.minusDays(3).atTime(21, 0).toString()
            ))
            .patch("/{id}", requestId)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("만료 시각만 수정 -> 200")
    void updateRequest_onlyExpiresAt_returns200() {
        LocalDate lessonDate = LocalDate.now().plusDays(15);
        createLessons(TEACHER_ID, lessonDate, 1, 2);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "원본 요청",
            "만료 시각 수정 테스트",
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(buildLessonExchangeRequestBody(
                lessonDate,
                "원본 요청",
                "만료 시각 수정 테스트",
                lessonDate.minusDays(3).atTime(20, 30)
            ))
            .patch("/{id}", requestId)
            .then()
            .statusCode(200)
            .body("expiresAt", startsWith(lessonDate.minusDays(3).atTime(20, 30).toString()));
    }

    @Test
    @DisplayName("만료 시각을 정책 위반 값으로 수정하면 -> 400")
    void updateRequest_expiresAtAfterPolicy_returns400() {
        LocalDate lessonDate = LocalDate.now().plusDays(16);
        createLessons(TEACHER_ID, lessonDate, 1);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "원본 요청",
            "만료 시각 정책 검증",
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(buildLessonExchangeRequestBody(
                lessonDate,
                "정책 위반 수정",
                "너무 늦은 만료 시각",
                lessonDate.minusDays(2).atTime(12, 0)
            ))
            .patch("/{id}", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("수업일을 다른 날짜로 수정 -> 200")
    void updateRequest_lessonDateToAnotherValidDate_returns200() {
        LocalDate originalDate = LocalDate.now().plusDays(17);
        LocalDate newDate = LocalDate.now().plusDays(18);
        createLessons(TEACHER_ID, originalDate, 1);
        createLessons(TEACHER_ID, newDate, 1);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            originalDate,
            "원본 요청",
            "날짜 수정 전",
            originalDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(buildLessonExchangeRequestBody(
                newDate,
                "날짜 수정 요청",
                "새 날짜로 변경",
                newDate.minusDays(3).atTime(21, 0)
            ))
            .patch("/{id}", requestId)
            .then()
            .statusCode(200)
            .body("lessonDate", equalTo(newDate.toString()))
            .body("$", not(hasKey("scope")))
            .body("$", not(hasKey("startPeriod")))
            .body("$", not(hasKey("endPeriod")));
    }

    @Test
    @DisplayName("다른 봉사자가 요청 수정 시도 -> 403")
    void updateRequest_asOtherVolunteer_returns403() {
        LocalDate lessonDate = LocalDate.now().plusDays(21);
        createLessons(TEACHER_ID, lessonDate, 1);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "원본 요청",
            "요청자 본인만 수정 가능",
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(buildLessonExchangeRequestBody(
                lessonDate,
                "타인 수정 시도",
                "수정 불가",
                lessonDate.minusDays(3).atTime(21, 0)
            ))
            .patch("/{id}", requestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("게스트는 수업 교환 요청을 수정할 수 없다 -> 403")
    void updateRequest_asGuest_returns403() {
        LocalDate lessonDate = LocalDate.now().plusDays(22);
        createLessons(TEACHER_ID, lessonDate, 1);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "게스트 수정 방지 요청",
            "게스트는 수정할 수 없습니다.",
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(requestId);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(buildLessonExchangeRequestBody(
                lessonDate,
                "게스트 수정 시도",
                "수정 불가",
                lessonDate.minusDays(3).atTime(21, 0)
            ))
            .patch("/{id}", requestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("승인된 요청은 수정할 수 없다 -> 409")
    void updateApprovedRequest_returns409() {
        LocalDate lessonDate = LocalDate.now().plusDays(23);
        createLessons(TEACHER_ID, lessonDate, 1);

        Long requestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "승인 전 요청",
            "승인 후 수정 불가 확인",
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
            .contentType(ContentType.JSON)
            .body(buildLessonExchangeRequestBody(
                lessonDate,
                "승인 후 수정 시도",
                "수정되면 안 됩니다.",
                lessonDate.minusDays(3).atTime(21, 0)
            ))
            .patch("/{id}", requestId)
            .then()
            .statusCode(409);
    }

    private void createLessons(Long teacherId, LocalDate lessonDate, int... periods) {
        Long subjectId = registerSubject(CLASSROOM_ID, teacherId);
        for (int period : periods) {
            switch (period) {
                case 1 -> registerLesson(subjectId, teacherId, lessonDate, "09:00:00", "09:50:00", 1);
                case 2 -> registerLesson(subjectId, teacherId, lessonDate, "10:00:00", "10:50:00", 2);
                case 3 -> registerLesson(subjectId, teacherId, lessonDate, "11:00:00", "11:50:00", 3);
                default -> throw new IllegalArgumentException("지원하지 않는 교시입니다: " + period);
            }
        }
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
