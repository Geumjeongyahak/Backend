package geumjeongyahak.e2e.request.lessonexchange;

import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@Tag("lesson-exchange-request")
@DisplayName("E2E: 수업 교환 요청 생성 테스트")
class LessonExchangeRequestCreateTest extends RequestBaseTest {

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
    @DisplayName("봉사자가 하루 단위 교환 요청 생성 -> 201, 필드 검증")
    void createDailyRequest_asVolunteer_returns201() {
        LocalDate lessonDate = LocalDate.now().plusDays(5);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        Long requestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "하루 단위 교환 요청",
                "content", "해당 날짜 수업 교환을 요청합니다.",
                "expiresAt", lessonDate.minusDays(3).atTime(23, 0).toString()
            ))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("classroomName", equalTo("벚꽃반"))
            .body("lessonDate", equalTo(lessonDate.toString()))
            .body("requestedByName", equalTo("홍길동"))
            .body("title", equalTo("하루 단위 교환 요청"))
            .body("status", equalTo("PENDING"))
            .body("$", not(hasKey("scope")))
            .body("$", not(hasKey("startPeriod")))
            .body("$", not(hasKey("endPeriod")))
            .extract()
            .jsonPath()
            .getLong("id");

        requestIds.add(requestId);
    }

    @Test
    @DisplayName("인증 없이 수업 교환 요청 생성 -> 401")
    void createRequest_unauthenticated_returns401() {
        LocalDate lessonDate = LocalDate.now().plusDays(5);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "제목",
                "content", "내용",
                "expiresAt", lessonDate.minusDays(3).atTime(23, 0).toString()
            ))
            .post()
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("게스트는 수업 교환 요청 생성 불가 -> 403")
    void createRequest_asGuest_returns403() {
        LocalDate lessonDate = LocalDate.now().plusDays(5);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "게스트 요청",
                "content", "게스트는 요청을 생성할 수 없습니다.",
                "expiresAt", lessonDate.minusDays(3).atTime(23, 0).toString()
            ))
            .post()
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("요청자 본인 수업이 없는 날짜로 생성 시도 -> 403")
    void createRequest_withoutOwnLessons_returns403() {
        LocalDate lessonDate = LocalDate.now().plusDays(5);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER2_ID);
        registerLesson(subjectId, TEACHER2_ID, lessonDate, "09:00:00", "10:00:00", 1);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "타인 수업 요청",
                "content", "내 수업이 아닌 일정으로 요청",
                "expiresAt", lessonDate.minusDays(3).atTime(23, 0).toString()
            ))
            .post()
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("같은 날짜에 진행 중 요청이 있으면 중복 생성 -> 409")
    void createRequest_duplicateActiveRequestOnSameDate_returns409() {
        LocalDate lessonDate = LocalDate.now().plusDays(7);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "09:50:00", 1);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "10:00:00", "10:50:00", 2);

        requestIds.add(createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "첫 요청",
            "중복 체크용",
            null,
            null,
            lessonDate.minusDays(3).atTime(22, 0)
        ));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "두 번째 요청",
                "content", "중복 생성 시도",
                "expiresAt", lessonDate.minusDays(3).atTime(21, 0).toString()
            ))
            .post()
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("title 이 빈 문자열이면 -> 400")
    void createRequest_blankTitle_returns400() {
        LocalDate lessonDate = LocalDate.now().plusDays(5);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "",
                "content", "내용",
                "expiresAt", lessonDate.minusDays(3).atTime(23, 0).toString()
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("만료 시각이 정책 허용 범위를 넘으면 -> 400")
    void createRequest_expiresAtAfterPolicy_returns400() {
        LocalDate lessonDate = LocalDate.now().plusDays(8);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "만료 정책 위반",
                "content", "너무 늦은 만료 시각",
                "expiresAt", lessonDate.minusDays(2).atTime(12, 0).toString()
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("요청 가능 시작일(today+4일)인 수업은 요청 가능 -> 201")
    void createRequest_atEarliestRequestableDate_returns201() {
        LocalDate lessonDate = LocalDate.now().plusDays(4);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        Long requestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "경계 날짜 요청",
                "content", "요청 가능 시작일 경계 테스트",
                "expiresAt", lessonDate.minusDays(3).atTime(23, 59, 59).toString()
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        requestIds.add(requestId);
    }

    @Test
    @DisplayName("요청 가능 시작일보다 하루 이른 수업은 요청 불가 -> 400")
    void createRequest_beforeEarliestRequestableDate_returns400() {
        LocalDate lessonDate = LocalDate.now().plusDays(3);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "정책 이전 날짜 요청",
                "content", "요청 가능 시작일보다 이른 날짜",
                "expiresAt", lessonDate.minusDays(2).atTime(23, 59, 59).toString()
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("만료 시각이 정책 상한과 정확히 같으면 허용 -> 201")
    void createRequest_expiresAtAtPolicyBoundary_returns201() {
        LocalDate lessonDate = LocalDate.now().plusDays(8);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        Long requestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "만료 경계 허용",
                "content", "정책 상한 시각과 같은 만료 시각",
                "expiresAt", lessonDate.minusDays(3).atTime(23, 59, 59).toString()
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        requestIds.add(requestId);
    }

    @Test
    @DisplayName("만료 시각이 수업일 자정과 같으면 -> 400")
    void createRequest_expiresAtAtLessonBoundary_returns400() {
        LocalDate lessonDate = LocalDate.now().plusDays(9);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "수업일 경계 만료",
                "content", "수업일 자정과 같은 만료 시각",
                "expiresAt", lessonDate.atStartOfDay().toString()
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("기존 요청이 REJECTED 상태면 같은 날짜에 새 요청 생성 가능 -> 201")
    void createRequest_afterRejectedRequest_returns201() {
        LocalDate lessonDate = LocalDate.now().plusDays(14);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "09:50:00", 1);

        Long rejectedRequestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "반려될 요청",
            "먼저 요청을 생성합니다.",
            null,
            null,
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(rejectedRequestId);
        markRequestStatus(rejectedRequestId, LessonExchangeRequestStatus.REJECTED);

        Long newRequestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "재요청",
                "content", "반려 후 같은 날짜 재요청",
                "expiresAt", lessonDate.minusDays(3).atTime(21, 0).toString()
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        requestIds.add(newRequestId);
    }

    @Test
    @DisplayName("기존 요청이 COMPLETED 상태면 같은 날짜에 새 요청 생성 가능 -> 201")
    void createRequest_afterCompletedRequest_returns201() {
        LocalDate lessonDate = LocalDate.now().plusDays(15);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "09:50:00", 1);

        Long completedRequestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "완료된 요청",
            "먼저 요청을 생성합니다.",
            null,
            null,
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(completedRequestId);
        markRequestStatus(completedRequestId, LessonExchangeRequestStatus.COMPLETED);

        Long newRequestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "완료 후 재요청",
                "content", "완료된 요청 이후 새 요청 생성",
                "expiresAt", lessonDate.minusDays(3).atTime(21, 0).toString()
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        requestIds.add(newRequestId);
    }

    @Test
    @DisplayName("기존 요청이 CANCELLED 상태면 같은 날짜에 새 요청 생성 가능 -> 201")
    void createRequest_afterCancelledRequest_returns201() {
        LocalDate lessonDate = LocalDate.now().plusDays(16);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "09:50:00", 1);

        Long cancelledRequestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "취소된 요청",
            "먼저 요청을 생성합니다.",
            null,
            null,
            lessonDate.minusDays(3).atTime(22, 0)
        );
        requestIds.add(cancelledRequestId);
        markRequestStatus(cancelledRequestId, LessonExchangeRequestStatus.CANCELLED);

        Long newRequestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "취소 후 재요청",
                "content", "취소된 요청 이후 새 요청 생성",
                "expiresAt", lessonDate.minusDays(3).atTime(21, 0).toString()
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        requestIds.add(newRequestId);
    }

    private void markRequestStatus(Long requestId, LessonExchangeRequestStatus status) {
        var request = lessonExchangeRequestRepository.findById(requestId).orElseThrow();
        ReflectionTestUtils.setField(request, "status", status);
        lessonExchangeRequestRepository.save(request);
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
