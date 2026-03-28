package sonmoeum.e2e.request.subjectexchange;

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
import sonmoeum.domain.request.repository.SubjectExchangeRequestRepository;
import sonmoeum.e2e.request.RequestBaseTest;

/**
 * 과목 교환 요청 생성 E2E 테스트.
 * init_data SUBJECT_ID=1 재사용 (과목 상태 미변경으로 격리 불필요).
 */
@Tag("subject-exchange-request")
@DisplayName("E2E: 과목 교환 요청 생성 테스트")
class SubjectExchangeRequestCreateTest extends RequestBaseTest {

    @Autowired
    private SubjectExchangeRequestRepository subjectExchangeRequestRepository;

    private Long createdRequestId;

    @AfterEach
    void cleanup() {
        if (createdRequestId != null) {
            subjectExchangeRequestRepository.deleteById(createdRequestId);
            createdRequestId = null;
        }
    }

    // ── 성공 ──────────────────────────────────────────────

    @Test
    @DisplayName("인증된 봉사자가 과목 교환 요청 생성 → 201, 필드 검증")
    void createRequest_authenticated_returns201() {
        createdRequestId = given()
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", SUBJECT_ID),
                entry("title", "과목 교환 요청"),
                entry("content", "과목 조정이 필요합니다.")
            ))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("subjectId", equalTo((int) SUBJECT_ID))
            .body("title", equalTo("과목 교환 요청"))
            .body("status", equalTo("PENDING"))
            .body("requestedByName", equalTo("홍길동"))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    // ── 인증 오류 ─────────────────────────────────────────

    @Test
    @DisplayName("인증 없이 과목 교환 요청 → 401")
    void createRequest_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/subject-exchange-requests")
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", SUBJECT_ID),
                entry("title", "제목"),
                entry("content", "내용")
            ))
            .post()
            .then()
            .statusCode(401);
    }

    // ── 도메인 오류 ───────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 과목 ID → 404")
    void createRequest_nonExistentSubject_returns404() {
        given()
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", 99999L),
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
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", SUBJECT_ID), entry("title", ""), entry("content", "내용")
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("content 가 빈 문자열 → 400")
    void createRequest_blankContent_returns400() {
        given()
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", SUBJECT_ID), entry("title", "제목"), entry("content", "")
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("subjectId 가 null → 400")
    void createRequest_nullSubjectId_returns400() {
        given()
            .basePath("/api/v1/subject-exchange-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(entry("title", "제목"), entry("content", "내용")))
            .post()
            .then()
            .statusCode(400);
    }
}
