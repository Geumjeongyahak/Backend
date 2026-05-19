package geumjeongyahak.e2e.request.purchase;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.containsString;

import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;

/**
 * 기자재 구입 요청 생성 E2E 테스트.
 * init_data 의 CLASSROOM_ID=1 (벚꽃반)을 재사용한다.
 * 분반 상태는 구입 요청으로 변경되지 않으므로 격리 문제 없음.
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
                entry("title", "교재 구입 요청"),
                entry("content", "한글 기초 교재가 필요합니다."),
                entry("classroomId", CLASSROOM_ID),
                entry("paymentMethod", "NORMAL"),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "한글 기초 교재"),
                    entry("reason", "수업 교재 부족"),
                    entry("price", 15000L)
                )))
            ))
            .post()
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("classroomId", equalTo((int) CLASSROOM_ID))
            .body("title", equalTo("교재 구입 요청"))
            .body("totalPrice", equalTo(15000))
            .body("paymentMethod", equalTo("NORMAL"))
            .body("status", equalTo("PENDING"))
            .body("requestedByName", equalTo("홍길동"))
            .body("items", hasSize(1))
            .body("items[0].name", equalTo("한글 기초 교재"))
            .body("items[0].price", equalTo(15000))
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
                entry("title", "칠판 구입"),
                entry("content", "교실용 칠판"),
                entry("classroomId", CLASSROOM_ID),
                entry("paymentMethod", "NORMAL"),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "칠판"),
                    entry("reason", "교실 비품 교체"),
                    entry("price", 50000L)
                )))
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Test
    @DisplayName("구입 요청 생성 시 품목 영수증 파일 ID 전달 → 상세 응답 item receipt 포함")
    void createRequest_withItemReceiptFileId_returnsReceipt() {
        String receiptFileId = uploadPurchaseReceipt();

        createdRequestId = given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "영수증 포함 구입 요청"),
                entry("content", "구입 요청 생성 시 영수증을 함께 첨부합니다."),
                entry("classroomId", CLASSROOM_ID),
                entry("paymentMethod", "NORMAL"),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "복사용지"),
                    entry("reason", "수업 자료 출력"),
                    entry("price", 10000L),
                    entry("receiptFileId", receiptFileId)
                )))
            ))
            .post()
            .then()
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .body("items[0].receiptFileUrl", containsString("/documents/purchase-items/"))
            .extract()
            .jsonPath()
            .getLong("id");

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}", createdRequestId)
            .then()
            .statusCode(200)
            .body("items[0].receiptFileUrl", containsString("/documents/purchase-items/"));
    }

    // ── 인증 오류 ─────────────────────────────────────────

    @Test
    @DisplayName("인증 없이 구입 요청 → 401")
    void createRequest_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/purchase-requests")
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "제목"),
                entry("content", "내용"),
                entry("classroomId", CLASSROOM_ID),
                entry("paymentMethod", "NORMAL"),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "품목"),
                    entry("reason", "사유"),
                    entry("price", 1000L)
                )))
            ))
            .post()
            .then()
            .statusCode(401);
    }

    // ── 도메인 오류 ───────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 분반 ID → 404")
    void createRequest_nonExistentClassroom_returns404() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "제목"),
                entry("content", "내용"),
                entry("classroomId", 99999L),
                entry("paymentMethod", "NORMAL"),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "품목"),
                    entry("reason", "사유"),
                    entry("price", 1000L)
                )))
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
                entry("title", ""),
                entry("content", "내용"),
                entry("classroomId", CLASSROOM_ID),
                entry("paymentMethod", "NORMAL"),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "품목"),
                    entry("reason", "사유"),
                    entry("price", 1000L)
                )))
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("items 가 빈 배열 → 400")
    void createRequest_emptyItems_returns400() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "제목"),
                entry("content", "내용"),
                entry("classroomId", CLASSROOM_ID),
                entry("paymentMethod", "NORMAL"),
                entry("items", List.of())
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
                entry("title", "제목"),
                entry("content", "내용"),
                entry("classroomId", CLASSROOM_ID),
                entry("paymentMethod", "NORMAL"),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "품목"),
                    entry("reason", "사유"),
                    entry("price", -100L)
                )))
            ))
            .post()
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("item name 이 빈 문자열 → 400")
    void createRequest_blankItemName_returns400() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "제목"),
                entry("content", "내용"),
                entry("classroomId", CLASSROOM_ID),
                entry("paymentMethod", "NORMAL"),
                entry("items", List.of(Map.ofEntries(
                    entry("name", ""),
                    entry("reason", "사유"),
                    entry("price", 1000L)
                )))
            ))
            .post()
            .then()
            .statusCode(400);
    }

    private String uploadPurchaseReceipt() {
        return given()
            .basePath("/api/v1/files")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.MULTIPART)
            .multiPart("file", "receipt.png", "receipt".getBytes(), "image/png")
            .post("/images/purchase-items")
            .then()
            .statusCode(201)
            .extract()
            .path("fileId");
    }
}
