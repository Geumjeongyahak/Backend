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
import static org.hamcrest.Matchers.startsWith;

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
                "expiresDate", lessonDate.minusDays(3).toString()
            ))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("classroomName", equalTo("벚꽃반"))
            .body("dailyScheduleId", equalTo(getDailyScheduleIdByLessonDate(lessonDate).intValue()))
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
                "expiresDate", lessonDate.minusDays(3).toString()
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
                "expiresDate", lessonDate.minusDays(3).toString()
            ))
            .post()
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("요청자 본인 DailySchedule이 없는 날짜로 생성 시도 -> 404")
    void createRequest_withoutOwnDailySchedule_returns404() {
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
                "expiresDate", lessonDate.minusDays(3).toString()
            ))
            .post()
            .then()
            .statusCode(404);
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
                "expiresDate", lessonDate.minusDays(3).toString()
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
                "expiresDate", lessonDate.minusDays(3).toString()
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("수업일 이전 만료일은 기존 3일 제한과 관계없이 허용 -> 201")
    void createRequest_expiresDateBeforeLessonDate_returns201() {
        LocalDate lessonDate = LocalDate.now().plusDays(8);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        Long requestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "만료 정책 위반",
                "content", "너무 늦은 만료 시각",
                "expiresDate", lessonDate.minusDays(2).toString()
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
    @DisplayName("수업 시작 전이면 4일 제한과 관계없이 요청 가능 -> 201")
    void createRequest_beforeLessonStart_returns201() {
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
                "expiresDate", lessonDate.minusDays(3).toString()
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
    @DisplayName("수업 시작 전이면 3일 뒤 수업도 요청 가능 -> 201")
    void createRequest_threeDaysBeforeLesson_returns201() {
        LocalDate lessonDate = LocalDate.now().plusDays(3);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        Long requestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "정책 이전 날짜 요청",
                "content", "요청 가능 시작일보다 이른 날짜",
                "expiresDate", lessonDate.minusDays(2).toString()
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
    @DisplayName("만료일이 수업일과 같으면 수업 시작 시각으로 설정 -> 201")
    void createRequest_expiresDateAtLessonDate_usesLessonStart() {
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
                "content", "수업일과 같은 만료일",
                "expiresDate", lessonDate.toString()
            ))
            .post()
            .then()
            .statusCode(201)
            .body("expiresAt", startsWith(lessonDate.atTime(9, 0).toString()))
            .extract()
            .jsonPath()
            .getLong("id");

        requestIds.add(requestId);
    }

    @Test
    @DisplayName("만료일이 수업일 이후이면 -> 400")
    void createRequest_expiresDateAfterLessonDate_returns400() {
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
                "content", "수업일 이후의 만료일",
                "expiresDate", lessonDate.plusDays(1).toString()
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("만료일에 날짜·시간 형식을 전달하면 -> 400")
    void createRequest_expiresDateWithDateTimeFormat_returns400() {
        LocalDate lessonDate = LocalDate.now().plusDays(10);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "잘못된 만료일 형식",
                "content", "날짜만 전달해야 합니다.",
                "expiresDate", lessonDate.minusDays(1).atStartOfDay().toString()
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("만료일을 생략하면 수업 시작 시각으로 자동 설정 -> 201")
    void createRequest_withoutExpiresDate_defaultsToLessonStart() {
        LocalDate lessonDate = LocalDate.now().plusDays(11);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        Long requestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "기본 만료 시각",
                "content", "만료일 생략"
            ))
            .post()
            .then()
            .statusCode(201)
            .body("expiresAt", startsWith(lessonDate.atTime(9, 0).toString()))
            .extract()
            .jsonPath()
            .getLong("id");

        requestIds.add(requestId);
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
                "expiresDate", lessonDate.minusDays(3).toString()
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
                "expiresDate", lessonDate.minusDays(3).toString()
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
                "expiresDate", lessonDate.minusDays(3).toString()
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
