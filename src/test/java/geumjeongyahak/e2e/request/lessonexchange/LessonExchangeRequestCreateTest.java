package geumjeongyahak.e2e.request.lessonexchange;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeScope;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

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
    @DisplayName("봉사자가 전체 교환 요청 생성 -> 201, 필드 검증")
    void createFullRequest_asVolunteer_returns201() {
        LocalDate lessonDate = LocalDate.now().plusDays(5);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "10:00:00", 1);

        Long requestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "전체 교환 요청",
                "content", "해당 날짜 전체 수업 교환을 요청합니다.",
                "scope", "FULL",
                "expiresAt", lessonDate.minusDays(3).atTime(23, 0).toString()
            ))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("classroomName", equalTo("벚꽃반"))
            .body("lessonDate", equalTo(lessonDate.toString()))
            .body("requestedByName", equalTo("홍길동"))
            .body("title", equalTo("전체 교환 요청"))
            .body("status", equalTo("PENDING"))
            .body("scope", equalTo("FULL"))
            .body("startPeriod", equalTo(null))
            .body("endPeriod", equalTo(null))
            .extract()
            .jsonPath()
            .getLong("id");

        requestIds.add(requestId);
    }

    @Test
    @DisplayName("봉사자가 부분 교환 요청 생성 -> 201, 교시 범위 저장")
    void createPartialRequest_asVolunteer_returns201() {
        LocalDate lessonDate = LocalDate.now().plusDays(6);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "09:50:00", 1);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "10:00:00", "10:50:00", 2);

        Long requestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "부분 교환 요청",
                "content", "1~2교시만 교환을 요청합니다.",
                "scope", "PARTIAL",
                "startPeriod", 1,
                "endPeriod", 2,
                "expiresAt", lessonDate.minusDays(3).atTime(22, 0).toString()
            ))
            .post()
            .then()
            .statusCode(201)
            .body("scope", equalTo("PARTIAL"))
            .body("startPeriod", equalTo(1))
            .body("endPeriod", equalTo(2))
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
                "scope", "FULL",
                "expiresAt", lessonDate.minusDays(3).atTime(23, 0).toString()
            ))
            .post()
            .then()
            .statusCode(401);
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
                "scope", "FULL",
                "expiresAt", lessonDate.minusDays(3).atTime(23, 0).toString()
            ))
            .post()
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("같은 날짜와 범위에 진행 중 요청이 있으면 중복 생성 -> 409")
    void createRequest_duplicateActiveRequest_returns409() {
        LocalDate lessonDate = LocalDate.now().plusDays(7);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "09:50:00", 1);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "10:00:00", "10:50:00", 2);

        requestIds.add(createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "첫 요청",
            "중복 체크용",
            LessonExchangeScope.PARTIAL,
            1,
            2,
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
                "scope", "PARTIAL",
                "startPeriod", 2,
                "endPeriod", 2,
                "expiresAt", lessonDate.minusDays(3).atTime(21, 0).toString()
            ))
            .post()
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("같은 날짜에 FULL 요청이 있으면 PARTIAL 요청 추가 생성 -> 409")
    void createRequest_partialAfterFull_returns409() {
        LocalDate lessonDate = LocalDate.now().plusDays(8);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "09:50:00", 1);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "10:00:00", "10:50:00", 2);

        requestIds.add(createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "전체 요청",
            "먼저 전체 요청을 생성합니다.",
            LessonExchangeScope.FULL,
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
                "title", "부분 요청",
                "content", "전체 요청과 같은 날짜에 부분 요청 추가",
                "scope", "PARTIAL",
                "startPeriod", 1,
                "endPeriod", 1,
                "expiresAt", lessonDate.minusDays(3).atTime(21, 0).toString()
            ))
            .post()
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("같은 날짜에 PARTIAL 요청이 있으면 FULL 요청 추가 생성 -> 409")
    void createRequest_fullAfterPartial_returns409() {
        LocalDate lessonDate = LocalDate.now().plusDays(9);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "09:50:00", 1);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "10:00:00", "10:50:00", 2);

        requestIds.add(createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "부분 요청",
            "먼저 부분 요청을 생성합니다.",
            LessonExchangeScope.PARTIAL,
            1,
            1,
            lessonDate.minusDays(3).atTime(22, 0)
        ));

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "전체 요청",
                "content", "부분 요청과 같은 날짜에 전체 요청 추가",
                "scope", "FULL",
                "expiresAt", lessonDate.minusDays(3).atTime(21, 0).toString()
            ))
            .post()
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("같은 날짜라도 교시가 겹치지 않는 PARTIAL 요청은 추가 생성 가능 -> 201")
    void createRequest_nonOverlappingPartial_returns201() {
        LocalDate lessonDate = LocalDate.now().plusDays(10);
        Long subjectId = registerSubject(CLASSROOM_ID, TEACHER_ID);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "09:00:00", "09:50:00", 1);
        registerLesson(subjectId, TEACHER_ID, lessonDate, "10:00:00", "10:50:00", 2);

        requestIds.add(createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "1교시 요청",
            "1교시만 요청합니다.",
            LessonExchangeScope.PARTIAL,
            1,
            1,
            lessonDate.minusDays(3).atTime(22, 0)
        ));

        Long requestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "2교시 요청",
                "content", "겹치지 않는 2교시 요청",
                "scope", "PARTIAL",
                "startPeriod", 2,
                "endPeriod", 2,
                "expiresAt", lessonDate.minusDays(3).atTime(21, 0).toString()
            ))
            .post()
            .then()
            .statusCode(201)
            .body("scope", equalTo("PARTIAL"))
            .body("startPeriod", equalTo(2))
            .body("endPeriod", equalTo(2))
            .extract()
            .jsonPath()
            .getLong("id");

        requestIds.add(requestId);
    }

    @Test
    @DisplayName("FULL 요청에 교시 범위를 함께 보내면 -> 400")
    void createRequest_fullWithPeriods_returns400() {
        LocalDate lessonDate = LocalDate.now().plusDays(11);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "전체 요청 교시 포함",
                "content", "FULL 요청에는 교시 범위를 보낼 수 없습니다.",
                "scope", "FULL",
                "startPeriod", 1,
                "endPeriod", 2,
                "expiresAt", lessonDate.minusDays(3).atTime(21, 0).toString()
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("PARTIAL 요청에서 시작 교시가 종료 교시보다 크면 -> 400")
    void createRequest_partialWithInvalidPeriodOrder_returns400() {
        LocalDate lessonDate = LocalDate.now().plusDays(12);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "잘못된 교시 순서",
                "content", "startPeriod > endPeriod",
                "scope", "PARTIAL",
                "startPeriod", 3,
                "endPeriod", 2,
                "expiresAt", lessonDate.minusDays(3).atTime(21, 0).toString()
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("PARTIAL 요청에서 교시 범위가 정책 밖이면 -> 400")
    void createRequest_partialWithOutOfRangePeriods_returns400() {
        LocalDate lessonDate = LocalDate.now().plusDays(13);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "교시 범위 초과",
                "content", "0교시부터 4교시까지는 허용되지 않습니다.",
                "scope", "PARTIAL",
                "startPeriod", 0,
                "endPeriod", 4,
                "expiresAt", lessonDate.minusDays(3).atTime(21, 0).toString()
            ))
            .post()
            .then()
            .statusCode(400);
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
                "scope", "FULL",
                "expiresAt", lessonDate.minusDays(3).atTime(23, 0).toString()
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("부분 교환에서 교시 범위가 없으면 -> 400")
    void createRequest_partialWithoutPeriods_returns400() {
        LocalDate lessonDate = LocalDate.now().plusDays(5);

        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "lessonDate", lessonDate.toString(),
                "title", "부분 교환",
                "content", "교시 누락",
                "scope", "PARTIAL",
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
                "scope", "FULL",
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
                "scope", "FULL",
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
                "scope", "FULL",
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
                "scope", "FULL",
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
                "scope", "FULL",
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
        registerLesson(subjectId, TEACHER_ID, lessonDate, "10:00:00", "10:50:00", 2);

        Long rejectedRequestId = createLessonExchangeRequest(
            getAuthHeader(volunteerToken),
            lessonDate,
            "반려될 요청",
            "먼저 요청을 생성합니다.",
            LessonExchangeScope.PARTIAL,
            1,
            1,
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
                "scope", "PARTIAL",
                "startPeriod", 1,
                "endPeriod", 1,
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
            LessonExchangeScope.FULL,
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
                "scope", "FULL",
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
            LessonExchangeScope.FULL,
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
                "scope", "FULL",
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
