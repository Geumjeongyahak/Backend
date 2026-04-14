package geumjeongyahak.e2e.request.purchase;

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
import geumjeongyahak.domain.request.repository.PurchaseRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;

/**
 * 기자재 구입 요청 생성 E2E 테스트.
 * init_data 의 SUBJECT_ID=1 (한글 기초, teacher01 담당)을 재사용한다.
 * 과목 상태는 구입 요청으로 변경되지 않으므로 격리 문제 없음.
 */
@Tag("purchase-request")
@DisplayName("E2E: 기자재 구입 요청 생성 테스트")
class PurchaseRequestCreateTest extends RequestBaseTest {

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    private Long createdRequestId;

    @AfterEach
    void cleanup() {
        if (createdRequestId != null) {
            purchaseRequestRepository.deleteById(createdRequestId);
            createdRequestId = null;
        }
    }

    // ── 성공 ──────────────────────────────────────────────

    @Test
    @DisplayName("인증된 봉사자가 구입 요청 생성 → 201, 필드 검증")
    void createRequest_asVolunteer_returns201() {
        createdRequestId = given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", SUBJECT_ID),
                entry("title", "교재 구입 요청"),
                entry("content", "한글 기초 교재가 필요합니다."),
                entry("price", 15000L)
            ))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("subjectId", equalTo((int) SUBJECT_ID))
            .body("title", equalTo("교재 구입 요청"))
            .body("price", equalTo(15000))
            .body("status", equalTo("PENDING"))
            .body("requestedByName", equalTo("홍길동"))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Test
    @DisplayName("관리자가 구입 요청 생성 → 201")
    void createRequest_asAdmin_returns201() {
        createdRequestId = given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", SUBJECT_ID),
                entry("title", "칠판 구입"),
                entry("content", "교실용 칠판"),
                entry("price", 50000L)
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    // ── 인증 오류 ─────────────────────────────────────────

    @Test
    @DisplayName("인증 없이 구입 요청 → 401")
    void createRequest_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/purchase-requests")
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", SUBJECT_ID),
                entry("title", "제목"),
                entry("content", "내용"),
                entry("price", 1000L)
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
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", 99999L),
                entry("title", "제목"),
                entry("content", "내용"),
                entry("price", 1000L)
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
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", SUBJECT_ID), entry("title", ""),
                entry("content", "내용"), entry("price", 1000L)
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("price 가 0 → 400 (@Min(1) validation)")
    void createRequest_priceZero_returns400() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", SUBJECT_ID), entry("title", "제목"),
                entry("content", "내용"), entry("price", 0L)
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("price 가 음수 → 400")
    void createRequest_negativePrice_returns400() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("subjectId", SUBJECT_ID), entry("title", "제목"),
                entry("content", "내용"), entry("price", -100L)
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("subjectId 가 null → 400")
    void createRequest_nullSubjectId_returns400() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "제목"), entry("content", "내용"), entry("price", 1000L)
            ))
            .post()
            .then()
            .statusCode(400);
    }
}
