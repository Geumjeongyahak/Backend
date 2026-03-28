package sonmoeum.e2e.request.lessonexchange;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import sonmoeum.domain.request.repository.LessonExchangeRequestRepository;
import sonmoeum.e2e.request.RequestBaseTest;

@Tag("lesson-exchange-request")
@DisplayName("E2E: 수업 교환 요청 생성 테스트")
class LessonExchangeRequestCreateTest extends RequestBaseTest {

    @Autowired
    private LessonExchangeRequestRepository lessonExchangeRequestRepository;

    private Long currentLessonId;
    private Long currentRequestId;

    @AfterEach
    void cleanup() {
        if (currentRequestId != null) {
            lessonExchangeRequestRepository.deleteById(currentRequestId);
            currentRequestId = null;
        }
        if (currentLessonId != null) {
            lessonHelper.deleteLesson(getAuthHeader(adminToken), currentLessonId);
            currentLessonId = null;
        }
    }

    // ── 성공 ──────────────────────────────────────────────

    @Test
    @DisplayName("인증된 봉사자가 수업 교환 요청 생성 → 201, 필드 검증")
    void createRequest_authenticated_returns201() {
        Long subjectId = lessonHelper.createSubjectAndGetId(
            getAuthHeader(adminToken), CLASSROOM_ID, TEACHER_ID);
        currentLessonId = lessonHelper.createLessonAndGetId(
            getAuthHeader(adminToken), subjectId, TEACHER_ID);

        currentRequestId = given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("lessonId", currentLessonId),
                entry("title", "수업 교환 요청"),
                entry("content", "개인 사정으로 수업 교환을 요청합니다.")
            ))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("lessonId", equalTo(currentLessonId.intValue()))
            .body("title", equalTo("수업 교환 요청"))
            .body("content", equalTo("개인 사정으로 수업 교환을 요청합니다."))
            .body("status", equalTo("PENDING"))
            .body("requestedByName", equalTo("홍길동"))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    // ── 인증 오류 ─────────────────────────────────────────

    @Test
    @DisplayName("인증 없이 수업 교환 요청 → 401")
    void createRequest_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("lessonId", 1L),
                entry("title", "제목"),
                entry("content", "내용")
            ))
            .post()
            .then()
            .statusCode(401);
    }

    // ── 도메인 오류 ───────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 수업 ID → 404")
    void createRequest_nonExistentLesson_returns404() {
        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("lessonId", 99999L),
                entry("title", "제목"),
                entry("content", "내용")
            ))
            .post()
            .then()
            .statusCode(404);
    }

    // ── 유효성 오류 ───────────────────────────────────────

    @Test
    @DisplayName("title 이 빈 문자열 → 400")
    void createRequest_blankTitle_returns400() {
        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(entry("lessonId", 1L), entry("title", ""), entry("content", "내용")))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("content 가 빈 문자열 → 400")
    void createRequest_blankContent_returns400() {
        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(entry("lessonId", 1L), entry("title", "제목"), entry("content", "")))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("lessonId 가 null → 400")
    void createRequest_nullLessonId_returns400() {
        given()
            .basePath("/api/v1/lesson-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(entry("title", "제목"), entry("content", "내용")))
            .post()
            .then()
            .statusCode(400);
    }
}
