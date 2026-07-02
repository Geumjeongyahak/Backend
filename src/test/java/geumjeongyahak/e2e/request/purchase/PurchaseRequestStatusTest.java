package geumjeongyahak.e2e.request.purchase;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.domain.vendor.repository.VendorBalanceHistoryRepository;
import geumjeongyahak.domain.vendor.repository.VendorRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;

/**
 * 기자재 구입 요청 승인·반려·조회 E2E 테스트.
 * 구입 요청은 승인해도 과목 상태를 변경하지 않으므로 side-effect 없음.
 */
@Tag("purchase-request")
@DisplayName("E2E: 기자재 구입 요청 승인·반려·조회 테스트")
class PurchaseRequestStatusTest extends RequestBaseTest {

    private static final String APPS_SCRIPT_BOT_EMAIL = "geumjeongyahak-apps-script-bot@gmail.com";
    private static final String APPS_SCRIPT_BOT_PASSWORD = "apps-script-bot123!";

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorBalanceHistoryRepository vendorBalanceHistoryRepository;

    private Long currentRequestId;
    private Long createdVendorId;
    private UUID registeredDriveFileId;

    @AfterEach
    void cleanup() {
        if (createdVendorId != null) {
            vendorBalanceHistoryRepository.deleteAllByVendor_Id(createdVendorId);
        }
        if (currentRequestId != null) {
            if (purchaseRequestRepository.existsById(currentRequestId)) {
                purchaseRequestRepository.deleteById(currentRequestId);
            }
            currentRequestId = null;
        }
        if (registeredDriveFileId != null && fileRepository.existsById(registeredDriveFileId)) {
            fileRepository.deleteById(registeredDriveFileId);
            registeredDriveFileId = null;
        }
        if (createdVendorId != null) {
            if (vendorRepository.existsById(createdVendorId)) {
                vendorRepository.deleteById(createdVendorId);
            }
            createdVendorId = null;
        }
    }

    private Long setupPendingRequest() {
        return createPurchaseRequest(
            getAuthHeader(volunteerToken), CLASSROOM_ID, "교재 구입", "교재가 필요합니다.", 20000L);
    }

    // ── 승인 (approve) ────────────────────────────────────

    @Test
    @DisplayName("관리자 구입 요청 승인 → 200, APPROVED, approvalAt 설정, note 저장")
    void approve_asAdmin_returns200() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "수업 운영에 필요한 물품으로 승인합니다."))
            .patch("/{requestId}/approve", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("approvalAt", notNullValue())
            .body("approvalByName", notNullValue())
            .body("note", equalTo("수업 운영에 필요한 물품으로 승인합니다."));
    }

    @Test
    @DisplayName("note 없이 승인 → 400")
    void approve_withoutNote_returns400() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of())
            .patch("/{requestId}/approve", currentRequestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("이미 처리된 구입 요청 재승인 → 409")
    void approve_alreadyProcessed_returns409() {
        currentRequestId = setupPendingRequest();

        given().basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "처리 확인"))
            .patch("/{requestId}/approve", currentRequestId)
            .then().statusCode(200);

        given().basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "재승인 사유"))
            .patch("/{requestId}/approve", currentRequestId)
            .then().statusCode(409);
    }

    @Test
    @DisplayName("봉사자 승인 시도 → 403")
    void approve_asVolunteer_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "승인 시도"))
            .patch("/{requestId}/approve", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("구입 요청 승인 → 거래처 잔액 미차감")
    void approve_doesNotDeductVendorBalance() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "거래처 잔액으로 승인"))
            .patch("/{requestId}/approve", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"));

        given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get("/{vendorId}", createdVendorId)
            .then()
            .statusCode(200)
            .body("balance", equalTo(100000));
    }

    @Test
    @DisplayName("결재 확인 시 잔액 부족 → 409")
    void confirm_withInsufficientBalance_returns409() {
        createdVendorId = createVendorAndCharge(1000L);
        currentRequestId = setupPurchasedRequest(createdVendorId, 20000L, null);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(409);

        given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get("/{vendorId}", createdVendorId)
            .then()
            .statusCode(200)
            .body("balance", equalTo(1000));
    }

    @Test
    @DisplayName("존재하지 않는 구입 요청 승인 → 404")
    void approve_notFound_returns404() {
        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "존재하지 않는 요청 승인"))
            .patch("/{requestId}/approve", 99999L)
            .then()
            .statusCode(404);
    }

    // ── 반려 (reject) ─────────────────────────────────────

    @Test
    @DisplayName("관리자 구입 요청 반려 → 200, REJECTED, note 저장")
    void reject_asAdmin_withNote_returns200() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "예산 초과로 반려합니다."))
            .patch("/{requestId}/reject", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("REJECTED"))
            .body("note", equalTo("예산 초과로 반려합니다."));
    }

    @Test
    @DisplayName("note 없이 반려 → 400")
    void reject_withoutNote_returns400() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of())
            .patch("/{requestId}/reject", currentRequestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("이미 처리된 요청 반려 → 409")
    void reject_alreadyProcessed_returns409() {
        currentRequestId = setupPendingRequest();

        given().basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "삭제 상태 전환"))
            .patch("/{requestId}/approve", currentRequestId)
            .then().statusCode(200);

        given().basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "뒤늦은 반려"))
            .patch("/{requestId}/reject", currentRequestId)
            .then().statusCode(409);
    }

    @Test
    @DisplayName("봉사자 반려 시도 → 403")
    void reject_asVolunteer_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "반려"))
            .patch("/{requestId}/reject", currentRequestId)
            .then()
            .statusCode(403);
    }

    // ── 삭제 (delete) ─────────────────────────────────────

    @Test
    @DisplayName("요청 작성자가 PENDING 구입 요청 삭제 → 204")
    void delete_asOwnerAndPending_returns204() {
        currentRequestId = setupPendingRequest();
        Long requestId = currentRequestId;

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{requestId}", requestId)
            .then()
            .statusCode(204);

        assertThat(purchaseRequestRepository.existsById(requestId)).isFalse();
        currentRequestId = null;
    }

    @Test
    @DisplayName("타인이 구입 요청 삭제 시도 → 403")
    void delete_asOtherVolunteer_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .delete("/{requestId}", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("이미 처리된 구입 요청 삭제 시도 → 409")
    void delete_processedRequest_returns409() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "삭제 불가 상태 전환"))
            .patch("/{requestId}/approve", currentRequestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{requestId}", currentRequestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("존재하지 않는 구입 요청 삭제 → 404")
    void delete_notFound_returns404() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{requestId}", 99999L)
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("관리자가 PENDING 구입 요청 삭제 → 204")
    void delete_asAdminAndPending_returns204() {
        currentRequestId = setupPendingRequest();
        Long requestId = currentRequestId;

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .delete("/{requestId}", requestId)
            .then()
            .statusCode(204);

        assertThat(purchaseRequestRepository.existsById(requestId)).isFalse();
        currentRequestId = null;
    }

    // ── 관리자 수정 (update) ─────────────────────────────

    @Test
    @DisplayName("Apps Script Bot이 PENDING 구입 요청 수정 → 200, 기본 정보와 품목 교체")
    void update_asAppsScriptBotAndPending_returns200() {
        currentRequestId = setupPendingRequest();
        String botAccessToken = loginAppsScriptBot();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(botAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", "시트 수정 구입 요청",
                "content", "Apps Script에서 수정한 내용입니다.",
                "items", List.of(Map.of(
                    "name", "수정된 품목",
                    "reason", "시트 수정 반영",
                    "quantity", 3,
                    "paymentType", "PREPAID"
                ))
            ))
            .patch("/{requestId}", currentRequestId)
            .then()
            .statusCode(200)
            .body("title", equalTo("시트 수정 구입 요청"))
            .body("content", equalTo("Apps Script에서 수정한 내용입니다."))
            .body("status", equalTo("PENDING"))
            .body("items", hasSize(1))
            .body("items[0].name", equalTo("수정된 품목"))
            .body("items[0].quantity", equalTo(3))
            .body("items[0].paymentType", equalTo("PREPAID"));
    }

    @Test
    @DisplayName("APPROVED 구입 요청 수정 → 409")
    void update_approvedRequest_returns409() {
        currentRequestId = setupPendingRequest();
        String botAccessToken = loginAppsScriptBot();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "수정 불가 상태 전환"))
            .patch("/{requestId}/approve", currentRequestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(botAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", "승인 후 수정",
                "content", "승인 후에는 수정할 수 없습니다.",
                "items", List.of(Map.of(
                    "name", "수정 시도 품목",
                    "reason", "상태 검증",
                    "quantity", 1,
                    "paymentType", "ACTUAL"
                ))
            ))
            .patch("/{requestId}", currentRequestId)
            .then()
            .statusCode(409);
    }

    // ── 구매 보고 (report) ────────────────────────────────

    @Test
    @DisplayName("구매 완료 보고 시 거래 라인과 영수증 저장 → 200")
    void report_withTransactionReceipt_returns200() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "구매 보고 테스트 승인"))
            .patch("/{requestId}/approve", currentRequestId)
            .then()
            .statusCode(200);

        String receiptFileId = uploadPurchaseReceipt();

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(reportBody(createdVendorId, 20000L, receiptFileId))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("PURCHASED"))
            .body("transactions", hasSize(1))
            .body("transactions[0].itemNames", hasSize(2))
            .body("transactions[0].receiptFileId", equalTo(receiptFileId));
    }

    @Test
    @DisplayName("Apps Script Bot이 Drive 영수증으로 구매 완료 보고 → 200, PURCHASED")
    void reportByAdmin_withAppsScriptBotAndDriveReceipt_returns200() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPendingRequest();
        String botAccessToken = loginAppsScriptBot();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "Bot 구매 보고 테스트 승인"))
            .patch("/{requestId}/approve", currentRequestId)
            .then()
            .statusCode(200);

        String receiptFileId = registerDriveReceipt(botAccessToken);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(botAccessToken))
            .contentType(ContentType.JSON)
            .body(reportBody(createdVendorId, 20000L, receiptFileId))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("PURCHASED"))
            .body("transactions", hasSize(1))
            .body("transactions[0].receiptFileId", equalTo(receiptFileId))
            .body("transactions[0].receiptFileUrl", equalTo("https://drive.google.com/file/d/apps-script-receipt/view?usp=sharing"));
    }

    @Test
    @DisplayName("권한 없는 사용자의 관리자 구매 완료 보고 → 403")
    void reportByAdmin_withoutManagePermission_returns403() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "권한 검증용 승인"))
            .patch("/{requestId}/approve", currentRequestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(managerToken))
            .contentType(ContentType.JSON)
            .body(reportBody(createdVendorId, 20000L, null))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(403);
    }

    // ── 재확인 요청 (reconfirmation) ───────────────────────

    @Test
    @DisplayName("요청 작성자가 PURCHASED 구입 요청 재확인 요청 → 204")
    void requestReconfirmation_asOwnerAndPurchased_returns204() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedRequest(createdVendorId, 20000L, null);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .post("/{requestId}/reconfirmation", currentRequestId)
            .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("타인이 구입 요청 재확인 요청 시도 → 403")
    void requestReconfirmation_asOtherVolunteer_returns403() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedRequest(createdVendorId, 20000L, null);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .post("/{requestId}/reconfirmation", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("PURCHASED가 아닌 구입 요청 재확인 요청 → 409")
    void requestReconfirmation_invalidStatus_returns409() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .post("/{requestId}/reconfirmation", currentRequestId)
            .then()
            .statusCode(409);
    }

    // ── 결재 확인 (confirm) ────────────────────────────────

    @Test
    @DisplayName("관리자가 PURCHASED 구입 요청 결재 확인 → 200, CONFIRMED")
    void confirm_asAdminAndPurchased_returns200() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedRequest(createdVendorId, 20000L, null);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("CONFIRMED"));

        given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get("/{vendorId}", createdVendorId)
            .then()
            .statusCode(200)
            .body("balance", equalTo(80000));
    }

    @Test
    @DisplayName("PURCHASED가 아닌 구입 요청 결재 확인 → 409")
    void confirm_invalidStatus_returns409() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("봉사자가 결재 확인 시도 → 403")
    void confirm_asVolunteer_returns403() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedRequest(createdVendorId, 20000L, null);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("구매 보고 후 거래처 비활성화 시 결재 확인 → 409, 잔액 유지")
    void confirm_inactiveVendor_returns409() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedRequest(createdVendorId, 20000L, null);

        given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("isActive", false))
            .patch("/{vendorId}", createdVendorId)
            .then()
            .statusCode(200)
            .body("isActive", equalTo(false));

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(409);

        given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get("/{vendorId}", createdVendorId)
            .then()
            .statusCode(200)
            .body("balance", equalTo(100000));
    }

    @Test
    @DisplayName("CONFIRMED 구입 요청 거래 수정 → 409")
    void updateItemReceipts_confirmedRequest_returns409() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedRequest(createdVendorId, 20000L, null);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("CONFIRMED"));

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(reportBody(createdVendorId, 25000L, null))
            .post("/{requestId}/item-receipts", currentRequestId)
            .then()
            .statusCode(409);
    }

    // ── 조회 ──────────────────────────────────────────────

    @Test
    @DisplayName("구입 요청 목록 기본 조회는 전체 요청, mine=true는 본인 요청만 조회")
    void getList_defaultSeesAll_mineTrueSeesOnlyOwn() {
        Long request1Id = createPurchaseRequest(
            getAuthHeader(volunteerToken), CLASSROOM_ID, "v1 요청", "내용1", 1000L);
        Long request2Id = createPurchaseRequest(
            getAuthHeader(volunteer2Token), CLASSROOM_ID, "v2 요청", "내용2", 2000L);

        try {
            List<Long> adminIds = given()
                .basePath("/api/v1/admin/purchase-requests")
                .header(AUTH_HEADER, getAuthHeader(adminToken))
                .get()
                .then().statusCode(200)
                .extract().jsonPath().getList("content.id", Long.class);
            assertThat(adminIds).contains(request1Id, request2Id);

            given()
                .basePath("/api/v1/admin/purchase-requests")
                .header(AUTH_HEADER, getAuthHeader(adminToken))
                .get()
                .then()
                .statusCode(200)
                .body("content.find { it.id == " + request1Id + " }.classroomId", equalTo((int) CLASSROOM_ID))
                .body("content.find { it.id == " + request1Id + " }.requestedById", equalTo((int) TEACHER_ID));

            List<Long> v1AllIds = given()
                .basePath("/api/v1/purchase-requests")
                .header(AUTH_HEADER, getAuthHeader(volunteerToken))
                .get()
                .then().statusCode(200)
                .extract().jsonPath().getList("content.id", Long.class);
            assertThat(v1AllIds).contains(request1Id, request2Id);

            List<Long> v1MineIds = given()
                .basePath("/api/v1/purchase-requests")
                .header(AUTH_HEADER, getAuthHeader(volunteerToken))
                .queryParam("mine", true)
                .get()
                .then().statusCode(200)
                .extract().jsonPath().getList("content.id", Long.class);
            assertThat(v1MineIds).contains(request1Id);
            assertThat(v1MineIds).doesNotContain(request2Id);

        } finally {
            purchaseRequestRepository.deleteById(request1Id);
            purchaseRequestRepository.deleteById(request2Id);
        }
    }

    @Test
    @DisplayName("status=PENDING 필터 → 승인된 요청 미포함")
    void getList_filteredByApprovedStatus_excludesPending() {
        currentRequestId = setupPendingRequest();

        // APPROVED 목록에는 PENDING 요청이 없어야 함
        List<Long> approvedIds = given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .queryParam("status", "APPROVED")
            .get()
            .then().statusCode(200)
            .extract().jsonPath().getList("id", Long.class);

        assertThat(approvedIds).doesNotContain(currentRequestId);
    }

    @Test
    @DisplayName("타인의 단건 조회 → 200")
    void getDetail_byNonOwner_returns200() {
        currentRequestId = createPurchaseRequest(
            getAuthHeader(volunteerToken), CLASSROOM_ID, "개인 요청", "내용", 5000L);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .get("/{requestId}", currentRequestId)
            .then()
            .statusCode(200)
            .body("id", equalTo(currentRequestId.intValue()));
    }

    @Test
    @DisplayName("게스트 구입 요청 목록 조회 → 403")
    void getList_asGuest_returns403() {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .get()
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("게스트 구입 요청 상세 조회 → 403")
    void getDetail_asGuest_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(guestToken))
            .get("/{requestId}", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("구매 완료 거래가 있는 단건 조회 → itemNames 직렬화")
    void getDetail_withPurchasedTransactions_serializesItemNames() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedRequest(createdVendorId, 20000L, null);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("PURCHASED"))
            .body("transactions", hasSize(1))
            .body("transactions[0].itemNames", hasSize(2))
            .body("transactions[0].itemNames[0]", equalTo("교재"))
            .body("transactions[0].itemNames[1]", equalTo("복사용지"));
    }

    @Test
    @DisplayName("인증 없이 목록 조회 → 401")
    void getList_unauthenticated_returns401() {
        given()
            .basePath("/api/v1/purchase-requests")
            .get()
            .then()
            .statusCode(401);
    }

    private Long setupPurchasedRequest(Long vendorId, long amount, String receiptFileId) {
        Long requestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "구매 보고 준비 승인"))
            .patch("/{requestId}/approve", requestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(reportBody(vendorId, amount, receiptFileId))
            .post("/{requestId}/report", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("PURCHASED"));

        return requestId;
    }

    private Long createVendorAndCharge(long amount) {
        Long vendorId = given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("name", "선결제 테스트 거래처"))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("amount", amount, "memo", "테스트 충전"))
            .post("/{vendorId}/charges", vendorId)
            .then()
            .statusCode(200);

        return vendorId;
    }

    private Map<String, Object> reportBody(Long vendorId, long amount, String receiptFileId) {
        Map<String, Object> transaction = new java.util.LinkedHashMap<>();
        transaction.put("vendorId", vendorId);
        transaction.put("itemNames", List.of("교재", "복사용지"));
        transaction.put("amount", amount);
        if (receiptFileId != null) {
            transaction.put("receiptFileId", receiptFileId);
        }
        return Map.of("transactions", List.of(transaction));
    }

    private String loginAppsScriptBot() {
        return given()
            .basePath("/api/v1/auth")
            .contentType(ContentType.JSON)
            .body(new LocalLoginRequest(APPS_SCRIPT_BOT_EMAIL, APPS_SCRIPT_BOT_PASSWORD))
            .post("/login")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("accessToken");
    }

    private String registerDriveReceipt(String accessToken) {
        String fileId = given()
            .basePath("/api/v1/files")
            .header(AUTH_HEADER, getAuthHeader(accessToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "driveUrl", "https://drive.google.com/file/d/apps-script-receipt/view?usp=sharing",
                "originalName", "apps-script-receipt.png",
                "mimeType", "image/png",
                "fileSize", 1024L
            ))
            .post("/drive")
            .then()
            .statusCode(201)
            .body("isGoogleDrive", equalTo(true))
            .body("url", equalTo("https://drive.google.com/file/d/apps-script-receipt/view?usp=sharing"))
            .extract()
            .path("fileId");
        registeredDriveFileId = UUID.fromString(fileId);
        return fileId;
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
