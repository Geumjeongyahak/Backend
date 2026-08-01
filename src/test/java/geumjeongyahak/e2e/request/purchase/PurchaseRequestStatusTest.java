package geumjeongyahak.e2e.request.purchase;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import java.time.LocalDate;
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
    private Long secondCreatedVendorId;
    private UUID registeredDriveFileId;
    private UUID uploadedProposalReceiptFileId;

    @AfterEach
    void cleanup() {
        if (createdVendorId != null) {
            vendorBalanceHistoryRepository.deleteAllByVendor_Id(createdVendorId);
        }
        if (secondCreatedVendorId != null) {
            vendorBalanceHistoryRepository.deleteAllByVendor_Id(secondCreatedVendorId);
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
        if (uploadedProposalReceiptFileId != null && fileRepository.existsById(uploadedProposalReceiptFileId)) {
            fileRepository.deleteById(uploadedProposalReceiptFileId);
            uploadedProposalReceiptFileId = null;
        }
        if (createdVendorId != null) {
            if (vendorRepository.existsById(createdVendorId)) {
                vendorRepository.deleteById(createdVendorId);
            }
            createdVendorId = null;
        }
        if (secondCreatedVendorId != null) {
            if (vendorRepository.existsById(secondCreatedVendorId)) {
                vendorRepository.deleteById(secondCreatedVendorId);
            }
            secondCreatedVendorId = null;
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
    @DisplayName("요청 작성자가 PENDING 구입 요청 삭제 → soft delete 후 조회 제외")
    void delete_asOwnerAndPending_returns204() {
        currentRequestId = setupPendingRequest();
        Long requestId = currentRequestId;

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{requestId}", requestId)
            .then()
            .statusCode(204);

        assertThat(purchaseRequestRepository.findById(requestId))
            .isPresent()
            .get()
            .satisfies(request -> {
                assertThat(request.isDeleted()).isTrue();
                assertThat(request.getDeletedAt()).isNotNull();
            });

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}", requestId)
            .then()
            .statusCode(404);

        List<Long> listedIds = given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get()
            .then()
            .statusCode(200)
            .extract().jsonPath().getList("content.id", Long.class);
        assertThat(listedIds).doesNotContain(requestId);
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
    @DisplayName("관리자가 PENDING 구입 요청 삭제 → soft delete")
    void delete_asAdminAndPending_returns204() {
        currentRequestId = setupPendingRequest();
        Long requestId = currentRequestId;

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .delete("/{requestId}", requestId)
            .then()
            .statusCode(204);

        assertThat(purchaseRequestRepository.findById(requestId))
            .isPresent()
            .get()
            .satisfies(request -> {
                assertThat(request.isDeleted()).isTrue();
                assertThat(request.getDeletedAt()).isNotNull();
            });
    }

    // ── 수정 (update) ────────────────────────────────────

    @Test
    @DisplayName("작성자가 본인의 PENDING 구입 요청 수정 → 200")
    void update_asRequesterAndPending_returns200() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "classroomId", 2L,
                "departmentId", 3L,
                "title", "작성자 수정 구입 요청",
                "content", "작성자가 수정한 내용입니다.",
                "items", List.of(Map.of(
                    "name", "작성자 수정 품목",
                    "reason", "작성자 수정 검증",
                    "quantity", 2
                ))
            ))
            .put("/{requestId}", currentRequestId)
            .then()
            .statusCode(200)
            .body("classroomId", equalTo(2))
            .body("departmentId", equalTo(3))
            .body("title", equalTo("작성자 수정 구입 요청"))
            .body("items[0].name", equalTo("작성자 수정 품목"));
    }

    @Test
    @DisplayName("다른 작성자가 PENDING 구입 요청 수정 → 403")
    void update_asOtherRequester_returns403() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "classroomId", CLASSROOM_ID,
                "departmentId", DEPARTMENT_ID,
                "title", "타인 수정 시도",
                "content", "다른 작성자는 수정할 수 없습니다.",
                "items", List.of(Map.of(
                    "name", "수정 시도 품목",
                    "reason", "접근 권한 검증",
                    "quantity", 1
                ))
            ))
            .put("/{requestId}", currentRequestId)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("Apps Script Bot이 PENDING 구입 요청 수정 → 200, 분반·부서·기본 정보·품목 교체")
    void update_asAppsScriptBotAndPending_returns200() {
        currentRequestId = setupPendingRequest();
        String botAccessToken = loginAppsScriptBot();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(botAccessToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "classroomId", 2L,
                "departmentId", 3L,
                "title", "시트 수정 구입 요청",
                "content", "Apps Script에서 수정한 내용입니다.",
                "items", List.of(Map.of(
                    "name", "수정된 품목",
                    "reason", "시트 수정 반영",
                    "quantity", 3
                ))
            ))
            .put("/{requestId}", currentRequestId)
            .then()
            .statusCode(200)
            .body("classroomId", equalTo(2))
            .body("classroomName", equalTo("장미반"))
            .body("departmentId", equalTo(3))
            .body("departmentName", equalTo("생활안전부"))
            .body("title", equalTo("시트 수정 구입 요청"))
            .body("content", equalTo("Apps Script에서 수정한 내용입니다."))
            .body("status", equalTo("PENDING"))
            .body("items", hasSize(1))
            .body("items[0].name", equalTo("수정된 품목"))
            .body("items[0].quantity", equalTo(3))
            .body("paymentType", equalTo("ACTUAL"));
    }

    @Test
    @DisplayName("구입 요청 수정 시 분반·담당 부서 누락 → 400")
    void update_missingClassroomAndDepartment_returns400() {
        currentRequestId = setupPendingRequest();

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", "필수값 누락 수정",
                "content", "분반과 담당 부서를 전송하지 않습니다.",
                "items", List.of(Map.of(
                    "name", "수정 품목",
                    "reason", "필수값 검증",
                    "quantity", 1
                ))
            ))
            .put("/{requestId}", currentRequestId)
            .then()
            .statusCode(400);
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
                "classroomId", CLASSROOM_ID,
                "departmentId", DEPARTMENT_ID,
                "title", "승인 후 수정",
                "content", "승인 후에는 수정할 수 없습니다.",
                "items", List.of(Map.of(
                    "name", "수정 시도 품목",
                    "reason", "상태 검증",
                    "quantity", 1
                ))
            ))
            .put("/{requestId}", currentRequestId)
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
            .body("transactions[0].itemNames", hasSize(1))
            .body("transactions[0].paymentMethod", nullValue())
            .body("transactions[0].receiptFileId", equalTo(receiptFileId));
    }

    @Test
    @DisplayName("선금 결제 구매 완료 보고에 거래가 여러 건이면 400")
    void report_prepaidWithMultipleTransactions_returns400() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = createPurchaseRequest(
            getAuthHeader(volunteerToken), CLASSROOM_ID, "선금 결제 구조 검증", "단일 거래만 허용", 20000L, "PREPAID");
        approvePurchaseRequest(currentRequestId);

        Map<String, Object> firstTransaction = transactionBody(
            createdVendorId, 10000L, List.of("선금 결제 구조 검증 품목"), null);
        Map<String, Object> secondTransaction = transactionBody(
            createdVendorId, 10000L, List.of("선금 결제 구조 검증 품목"), null);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", List.of(firstTransaction, secondTransaction)))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("선금 결제 구매 완료 보고에 지급 구분이 없으면 400")
    void report_prepaidWithoutPaymentMethod_returns400() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = createPurchaseRequest(
            getAuthHeader(volunteerToken), CLASSROOM_ID, "선금 지급 구분 검증", "지급 구분 필수", 20000L, "PREPAID");
        approvePurchaseRequest(currentRequestId);
        Map<String, Object> transaction = transactionBody(
            createdVendorId, 20000L, List.of("선금 지급 구분 검증 품목"), null);
        transaction.remove("paymentMethod");

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", List.of(transaction)))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("실 결제의 거래 수가 신청 품목 수와 다르면 400")
    void report_actualWithMissingItemTransaction_returns400() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = createActualPurchaseRequestWithTwoItems();
        approvePurchaseRequest(currentRequestId);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", List.of(
                transactionBody(createdVendorId, 20000L, List.of("교재"), null)
            )))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("실 결제 거래 한 건에 품목이 여러 개이면 400")
    void report_actualWithMultipleItemsInOneTransaction_returns400() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = createActualPurchaseRequestWithTwoItems();
        approvePurchaseRequest(currentRequestId);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", List.of(
                transactionBody(createdVendorId, 20000L, List.of("교재", "문구"), null)
            )))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(400);
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
    @DisplayName("실 결제는 품의 정보가 없어도 결재 확인 → 200, CONFIRMED")
    void confirm_actualWithoutProposal_returns200() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedRequestWithoutProposal(createdVendorId, 20000L, null);

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
    @DisplayName("선금 결제는 품의 정보가 없으면 결재 확인을 거부한다")
    void confirm_prepaidWithoutProposal_returns409() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedPrepaidRequestWithoutProposal(createdVendorId, 20000L, null);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(409)
            .body("code", equalTo("PR-020"));

        assertVendorBalance(createdVendorId, 100000);
        assertPurchaseRequestStatus(currentRequestId, "PURCHASED");
    }

    @Test
    @DisplayName("품의 최종 확인 필수값이 누락되면 결재 확인을 거부한다")
    void confirm_withIncompleteProposal_returns409() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedPrepaidRequestWithoutProposal(createdVendorId, 20000L, null);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of())
            .put("/{requestId}/proposal", currentRequestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(409)
            .body("code", equalTo("PR-021"));

        assertVendorBalance(createdVendorId, 100000);
        assertPurchaseRequestStatus(currentRequestId, "PURCHASED");
    }

    @Test
    @DisplayName("완료 요청일이 없으면 결재 확인을 거부한다")
    void confirm_withoutCompletionDate_returns409() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedPrepaidRequestWithoutProposal(createdVendorId, 20000L, null);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "proposalDate", LocalDate.now().toString(),
                "proposalAmount", 20000L,
                "paymentAccount", "NATIONAL_SUBSIDY_04",
                "items", List.of(Map.of(
                    "content", "완료 요청일 검증 품목",
                    "quantity", 1,
                    "estimatedUnitPrice", 20000L
                ))
            ))
            .put("/{requestId}/proposal", currentRequestId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(409)
            .body("code", equalTo("PR-021"));

        assertVendorBalance(createdVendorId, 100000);
        assertPurchaseRequestStatus(currentRequestId, "PURCHASED");
    }

    @Test
    @DisplayName("품의금액과 품목 예상 금액 합계가 다르면 결재 확인을 거부한다")
    void confirm_withProposalItemAmountMismatch_returns409() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedPrepaidRequestWithoutProposal(createdVendorId, 20000L, null);
        saveProposal(currentRequestId, 20000L, 19000L);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(409)
            .body("code", equalTo("PR-022"));

        assertVendorBalance(createdVendorId, 100000);
        assertPurchaseRequestStatus(currentRequestId, "PURCHASED");
    }

    @Test
    @DisplayName("품의금액과 실제 결제 금액이 다르면 결재 확인을 거부한다")
    void confirm_withProposalPaymentAmountMismatch_returns409() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = setupPurchasedPrepaidRequestWithoutProposal(createdVendorId, 20000L, null);
        saveProposal(currentRequestId, 19000L, 19000L);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(409)
            .body("code", equalTo("PR-023"));

        assertVendorBalance(createdVendorId, 100000);
        assertPurchaseRequestStatus(currentRequestId, "PURCHASED");
    }

    @Test
    @DisplayName("선금 결제 결재 확인 시 거래처 잔액을 충전한다")
    void confirm_prepaid_chargesVendorBalance() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = createPurchaseRequest(
            getAuthHeader(volunteerToken), CLASSROOM_ID, "선금 결제 충전", "결재 확인 시 충전", 20000L, "PREPAID");
        approvePurchaseRequest(currentRequestId);
        String receiptFileId = uploadPurchaseReceipt();

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", List.of(transactionBody(
                createdVendorId, 20000L, List.of("선금 결제 충전 품목"), receiptFileId))))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(200);

        saveCompleteProposal(currentRequestId, 20000L);

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
            .body("balance", equalTo(120000));
    }

    @Test
    @DisplayName("선금 결제에 활성 영수증이 없으면 결재 확인을 거부한다")
    void confirm_prepaidWithoutReceipt_returns409() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = createPurchaseRequest(
            getAuthHeader(volunteerToken), CLASSROOM_ID, "선금 영수증 검증", "영수증 필수", 20000L, "PREPAID");
        approvePurchaseRequest(currentRequestId);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", List.of(transactionBody(
                createdVendorId, 20000L, List.of("선금 영수증 검증 품목"), null))))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(200);

        saveCompleteProposal(currentRequestId, 20000L);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(409)
            .body("code", equalTo("PR-017"));
    }

    @Test
    @DisplayName("선금 결제는 품의 단계의 활성 영수증만 있어도 결재 확인할 수 있다")
    void confirm_prepaidWithProposalReceipt_returns200() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = createPurchaseRequest(
            getAuthHeader(volunteerToken),
            CLASSROOM_ID,
            "품의 영수증 선금 결제",
            "품의 단계 영수증 인정",
            20000L,
            "PREPAID");
        approvePurchaseRequest(currentRequestId);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", List.of(transactionBody(
                createdVendorId, 20000L, List.of("품의 영수증 선금 결제 품목"), null))))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(200);

        saveCompleteProposal(currentRequestId, 20000L);
        uploadedProposalReceiptFileId = UUID.fromString(uploadPurchaseReceipt());

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("fileId", uploadedProposalReceiptFileId.toString()))
            .post("/{requestId}/proposal/receipts", currentRequestId)
            .then()
            .statusCode(201);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("CONFIRMED"));

        assertVendorBalance(createdVendorId, 120000);
    }

    @Test
    @DisplayName("선금 결제 거래처가 비활성이면 충전과 결재 확인을 거부한다")
    void confirm_prepaidWithInactiveVendor_returns409() {
        createdVendorId = createVendorAndCharge(100000L);
        currentRequestId = createPurchaseRequest(
            getAuthHeader(volunteerToken), CLASSROOM_ID, "선금 비활성 거래처", "비활성 충전 차단", 20000L, "PREPAID");
        approvePurchaseRequest(currentRequestId);
        String receiptFileId = uploadPurchaseReceipt();

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", List.of(transactionBody(
                createdVendorId, 20000L, List.of("선금 비활성 거래처 품목"), receiptFileId))))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(200);

        saveCompleteProposal(currentRequestId, 20000L);

        given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("isActive", false))
            .patch("/{vendorId}", createdVendorId)
            .then()
            .statusCode(200);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(409);

        assertVendorBalance(createdVendorId, 100000);
    }

    @Test
    @DisplayName("실 결제는 품목별로 다른 거래처를 허용하고 각 잔액을 차감한다")
    void confirm_actualWithDifferentVendors_deductsEachVendorBalance() {
        createdVendorId = createVendorAndCharge(100000L);
        secondCreatedVendorId = createVendorAndCharge(50000L);
        currentRequestId = createActualPurchaseRequestWithTwoItems();
        approvePurchaseRequest(currentRequestId);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", List.of(
                transactionBody(createdVendorId, 12000L, List.of("교재"), null),
                transactionBody(secondCreatedVendorId, 8000L, List.of("문구"), null)
            )))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(200)
            .body("transactions", hasSize(2))
            .body("transactions[0].paymentMethod", nullValue())
            .body("transactions[1].paymentMethod", nullValue());

        saveCompleteProposal(currentRequestId, 20000L);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("CONFIRMED"));

        assertVendorBalance(createdVendorId, 88000);
        assertVendorBalance(secondCreatedVendorId, 42000);
    }

    @Test
    @DisplayName("실 결제 거래처 중 하나가 잔액 부족이면 모든 차감을 롤백한다")
    void confirm_actualWithInsufficientSecondVendor_rollsBackAllDeductions() {
        createdVendorId = createVendorAndCharge(100000L);
        secondCreatedVendorId = createVendorAndCharge(1000L);
        currentRequestId = createActualPurchaseRequestWithTwoItems();
        approvePurchaseRequest(currentRequestId);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", List.of(
                transactionBody(createdVendorId, 12000L, List.of("교재"), null),
                transactionBody(secondCreatedVendorId, 8000L, List.of("문구"), null)
            )))
            .post("/{requestId}/report", currentRequestId)
            .then()
            .statusCode(200);

        saveCompleteProposal(currentRequestId, 20000L);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", currentRequestId)
            .then()
            .statusCode(409);

        assertVendorBalance(createdVendorId, 100000);
        assertVendorBalance(secondCreatedVendorId, 1000);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}", currentRequestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("PURCHASED"));
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
            .body("transactions[0].itemNames", hasSize(1))
            .body("transactions[0].itemNames[0]", equalTo("교재 구입 품목"))
            .body("transactions[0].paymentMethod", nullValue());
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
        Long requestId = setupPurchasedRequestWithoutProposal(vendorId, amount, receiptFileId);
        saveCompleteProposal(requestId, amount);
        return requestId;
    }

    private Long setupPurchasedRequestWithoutProposal(Long vendorId, long amount, String receiptFileId) {
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

    private Long setupPurchasedPrepaidRequestWithoutProposal(
        Long vendorId,
        long amount,
        String receiptFileId
    ) {
        Long requestId = createPurchaseRequest(
            getAuthHeader(volunteerToken),
            CLASSROOM_ID,
            "선금 결재 확인",
            "선금 품의 검증",
            amount,
            "PREPAID"
        );
        approvePurchaseRequest(requestId);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", List.of(transactionBody(
                vendorId,
                amount,
                List.of("선금 결재 확인 품목"),
                receiptFileId
            ))))
            .post("/{requestId}/report", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo("PURCHASED"));

        return requestId;
    }

    private void saveCompleteProposal(Long requestId, long amount) {
        saveProposal(requestId, amount, amount);
    }

    private void saveProposal(Long requestId, long proposalAmount, long estimatedUnitPrice) {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "proposalDate", LocalDate.now().toString(),
                "completionDate", LocalDate.now().toString(),
                "proposalAmount", proposalAmount,
                "paymentAccount", "NATIONAL_SUBSIDY_04",
                "items", List.of(Map.of(
                    "content", "최종 확인 품목",
                    "quantity", 1,
                    "estimatedUnitPrice", estimatedUnitPrice
                ))
            ))
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(200);
    }

    private void assertPurchaseRequestStatus(Long requestId, String expectedStatus) {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}", requestId)
            .then()
            .statusCode(200)
            .body("status", equalTo(expectedStatus));
    }

    private void approvePurchaseRequest(Long requestId) {
        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "구매 완료 보고 구조 검증"))
            .patch("/{requestId}/approve", requestId)
            .then()
            .statusCode(200);
    }

    private Long createActualPurchaseRequestWithTwoItems() {
        return given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "title", "실 결제 다중 거래처 검증",
                "content", "품목별 거래처 검증",
                "classroomId", CLASSROOM_ID,
                "departmentId", DEPARTMENT_ID,
                "paymentType", "ACTUAL",
                "items", List.of(
                    Map.of("name", "교재", "reason", "수업 자료", "quantity", 1),
                    Map.of("name", "문구", "reason", "수업 용품", "quantity", 1)
                )
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    private Map<String, Object> transactionBody(
        Long vendorId,
        long amount,
        List<String> itemNames,
        String receiptFileId
    ) {
        Map<String, Object> transaction = new java.util.LinkedHashMap<>();
        transaction.put("vendorId", vendorId);
        transaction.put("itemNames", itemNames);
        transaction.put("amount", amount);
        transaction.put("paymentMethod", "CARD");
        if (receiptFileId != null) {
            transaction.put("receiptFileId", receiptFileId);
        }
        return transaction;
    }

    private void assertVendorBalance(Long vendorId, int expectedBalance) {
        given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .get("/{vendorId}", vendorId)
            .then()
            .statusCode(200)
            .body("balance", equalTo(expectedBalance));
    }

    private Long createVendorAndCharge(long amount) {
        Long vendorId = given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("name", "결제 테스트 거래처 " + System.nanoTime()))
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
        Map<String, Object> transaction = transactionBody(
            vendorId, amount, List.of("교재 구입 품목"), receiptFileId);
        transaction.remove("paymentMethod");
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
