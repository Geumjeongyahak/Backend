package geumjeongyahak.e2e.request.purchase;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestProposalReceiptRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@Tag("purchase-request")
@DisplayName("E2E: 품의 정보 저장 및 조회 테스트")
class PurchaseRequestProposalTest extends RequestBaseTest {

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private PurchaseRequestProposalReceiptRepository proposalReceiptRepository;

    private final List<Long> createdRequestIds = new ArrayList<>();
    private final List<UUID> uploadedFileIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        createdRequestIds.reversed().forEach(purchaseRequestRepository::deleteById);
        createdRequestIds.clear();
        uploadedFileIds.stream()
            .filter(fileRepository::existsById)
            .forEach(fileRepository::deleteById);
        uploadedFileIds.clear();
    }

    @Test
    @DisplayName("작성자가 전체 품의 정보를 저장하면 계산값과 상세 응답에 반영된다")
    void saveProposal_asAuthor_returnsCalculatedValues() {
        Long requestId = createPurchaseRequest("품의 저장 테스트");
        LocalDate proposalDate = LocalDate.now();

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(fullProposalBody(proposalDate))
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(200)
            .body("proposalNumber", equalTo(
                "%d품-국비04-01".formatted(proposalDate.getYear())
            ))
            .body("requestDepartmentId", equalTo(2))
            .body("paymentAccount", equalTo("NATIONAL_SUBSIDY_04"))
            .body("budget.itemCategory", equalTo("TEXTBOOK"))
            .body("items", hasSize(1))
            .body("items[0].expectedAmount", equalTo(2000))
            .body("items[0].sortOrder", equalTo(0));

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}", requestId)
            .then()
            .statusCode(200)
            .body("proposal.proposalNumber", equalTo(
                "%d품-국비04-01".formatted(proposalDate.getYear())
            ))
            .body("proposal.items[0].expectedAmount", equalTo(2000));
    }

    @Test
    @DisplayName("빈 요청으로도 품의 정보를 중간 저장할 수 있다")
    void saveProposal_withEmptyBody_allowsDraft() {
        Long requestId = createPurchaseRequest("빈 품의 저장 테스트");

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of())
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(200)
            .body("proposalNumber", nullValue())
            .body("proposalDate", nullValue())
            .body("paymentAccount", nullValue())
            .body("budget", nullValue())
            .body("items", hasSize(0));
    }

    @Test
    @DisplayName("품의 단계에 여러 영수증을 첨부하고 개별 영수증을 소프트 삭제할 수 있다")
    void manageProposalReceipts_returnsOnlyActiveReceipts() {
        Long requestId = createPurchaseRequest("품의 영수증 관리 테스트");
        UUID firstFileId = uploadPurchaseReceipt("first-receipt.png");
        UUID secondFileId = uploadPurchaseReceipt("second-receipt.png");

        Long firstReceiptId = given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("fileId", firstFileId.toString()))
            .post("/{requestId}/proposal/receipts", requestId)
            .then()
            .statusCode(201)
            .body("receipts", hasSize(1))
            .body("receipts[0].fileId", equalTo(firstFileId.toString()))
            .extract()
            .jsonPath()
            .getLong("receipts[0].id");

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("fileId", secondFileId.toString()))
            .post("/{requestId}/proposal/receipts", requestId)
            .then()
            .statusCode(201)
            .body("receipts", hasSize(2))
            .body("receipts[1].fileId", equalTo(secondFileId.toString()))
            .body("receipts[1].sortOrder", equalTo(1));

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("fileId", firstFileId.toString()))
            .post("/{requestId}/proposal/receipts", requestId)
            .then()
            .statusCode(201)
            .body("receipts", hasSize(2));

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{requestId}/proposal/receipts/{receiptId}", requestId, firstReceiptId)
            .then()
            .statusCode(204);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}", requestId)
            .then()
            .statusCode(200)
            .body("proposal.receipts", hasSize(1))
            .body("proposal.receipts[0].fileId", equalTo(secondFileId.toString()));

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{requestId}/proposal/receipts/{receiptId}", requestId, firstReceiptId)
            .then()
            .statusCode(404)
            .body("code", equalTo("PR-019"));

        assertThat(proposalReceiptRepository.findById(firstReceiptId).orElseThrow().isDeleted()).isTrue();
        assertThat(fileRepository.findById(firstFileId).orElseThrow().isDeleted()).isFalse();
    }

    @Test
    @DisplayName("다른 작성자는 품의 영수증을 관리할 수 없고 관리자는 관리할 수 있다")
    void manageProposalReceipts_checksAuthorAndAdminAccess() {
        Long requestId = createPurchaseRequest("품의 영수증 권한 테스트");
        UUID fileId = uploadPurchaseReceipt("admin-receipt.png");

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("fileId", fileId.toString()))
            .post("/{requestId}/proposal/receipts", requestId)
            .then()
            .statusCode(403)
            .body("code", equalTo("PR-002"));

        Long receiptId = given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("fileId", fileId.toString()))
            .post("/{requestId}/proposal/receipts", requestId)
            .then()
            .statusCode(201)
            .body("receipts", hasSize(1))
            .extract()
            .jsonPath()
            .getLong("receipts[0].id");

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .delete("/{requestId}/proposal/receipts/{receiptId}", requestId, receiptId)
            .then()
            .statusCode(403)
            .body("code", equalTo("PR-002"));

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .delete("/{requestId}/proposal/receipts/{receiptId}", requestId, receiptId)
            .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("CONFIRMED 상태에서는 품의 영수증을 첨부하거나 삭제할 수 없다")
    void manageProposalReceipts_whenConfirmed_returns409() {
        Long requestId = createPurchaseRequest("품의 영수증 확정 상태 테스트");
        UUID attachedFileId = uploadPurchaseReceipt("attached-receipt.png");
        UUID newFileId = uploadPurchaseReceipt("new-receipt.png");

        Long receiptId = given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("fileId", attachedFileId.toString()))
            .post("/{requestId}/proposal/receipts", requestId)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("receipts[0].id");

        var purchaseRequest = purchaseRequestRepository.findById(requestId).orElseThrow();
        purchaseRequest.confirm();
        purchaseRequestRepository.saveAndFlush(purchaseRequest);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("fileId", newFileId.toString()))
            .post("/{requestId}/proposal/receipts", requestId)
            .then()
            .statusCode(409)
            .body("code", equalTo("PR-018"));

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{requestId}/proposal/receipts/{receiptId}", requestId, receiptId)
            .then()
            .statusCode(409)
            .body("code", equalTo("PR-018"));
    }

    @Test
    @DisplayName("기존 품의 정보는 요청 본문의 전체 상태로 교체하고 예산·품목을 제거할 수 있다")
    void saveProposal_again_replacesAndClearsExistingProposal() {
        Long requestId = createPurchaseRequest("품의 전체 교체 테스트");
        saveProposal(requestId, fullProposalBody(LocalDate.now()));

        Map<String, Object> replacement = Map.of(
            "overview", "수정된 품의 개요",
            "budget", Map.of(
                "itemCategory", "RENTAL",
                "calculationDetail", "BANNER"
            ),
            "items", List.of(Map.ofEntries(
                entry("content", "현수막"),
                entry("specification", "개"),
                entry("quantity", 2),
                entry("estimatedUnitPrice", 5_000L)
            ))
        );

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(replacement)
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(200)
            .body("overview", equalTo("수정된 품의 개요"))
            .body("policyProject", nullValue())
            .body("proposalNumber", nullValue())
            .body("budget.itemCategory", equalTo("RENTAL"))
            .body("items", hasSize(1))
            .body("items[0].content", equalTo("현수막"))
            .body("items[0].expectedAmount", equalTo(10_000));

        Map<String, Object> cleared = new HashMap<>();
        cleared.put("budget", null);
        cleared.put("items", List.of());

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(cleared)
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(200)
            .body("budget", nullValue())
            .body("items", hasSize(0));
    }

    @Test
    @DisplayName("더 과거 품의일자를 저장하면 기존 품의번호가 뒤로 밀린다")
    void saveBackdatedProposal_shiftsLaterProposalNumber() {
        Long laterRequestId = createPurchaseRequest("이후 품의");
        Long earlierRequestId = createPurchaseRequest("이전 품의");
        LocalDate laterDate = LocalDate.now();
        LocalDate earlierDate = laterDate.minusDays(1);

        saveMinimalProposal(laterRequestId, laterDate);
        saveMinimalProposal(earlierRequestId, earlierDate);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}", laterRequestId)
            .then()
            .statusCode(200)
            .body("proposal.proposalNumber", equalTo(
                "%d품-국비04-02".formatted(laterDate.getYear())
            ));
    }

    @Test
    @DisplayName("결제 통장이 다르면 같은 연도에도 각각 01부터 시작한다")
    void proposalNumber_usesSeparateSequencePerPaymentAccount() {
        Long nationalRequestId = createPurchaseRequest("국비 품의");
        Long districtRequestId = createPurchaseRequest("구비 품의");
        LocalDate proposalDate = LocalDate.now();

        saveMinimalProposal(nationalRequestId, proposalDate, "NATIONAL_SUBSIDY_04");
        saveMinimalProposal(districtRequestId, proposalDate, "DISTRICT_BUDGET_01");

        assertProposalNumber(
            nationalRequestId,
            "%d품-국비04-01".formatted(proposalDate.getYear())
        );
        assertProposalNumber(
            districtRequestId,
            "%d품-구비01-01".formatted(proposalDate.getYear())
        );
    }

    @Test
    @DisplayName("연도가 다르면 같은 결제 통장도 각각 01부터 시작한다")
    void proposalNumber_resetsEveryYear() {
        Long previousYearRequestId = createPurchaseRequest("이전 연도 품의");
        Long currentYearRequestId = createPurchaseRequest("현재 연도 품의");
        LocalDate currentDate = LocalDate.now();
        LocalDate previousDate = currentDate.minusYears(1);

        saveMinimalProposal(previousYearRequestId, previousDate);
        saveMinimalProposal(currentYearRequestId, currentDate);

        assertProposalNumber(
            previousYearRequestId,
            "%d품-국비04-01".formatted(previousDate.getYear())
        );
        assertProposalNumber(
            currentYearRequestId,
            "%d품-국비04-01".formatted(currentDate.getYear())
        );
    }

    @Test
    @DisplayName("품의일자가 같으면 결제 신청 생성 순서로 번호를 계산한다")
    void proposalNumber_usesStableOrderForSameDate() {
        Long firstRequestId = createPurchaseRequest("동일 일자 첫 품의");
        Long secondRequestId = createPurchaseRequest("동일 일자 둘째 품의");
        LocalDate proposalDate = LocalDate.now();

        saveMinimalProposal(secondRequestId, proposalDate);
        saveMinimalProposal(firstRequestId, proposalDate);

        assertProposalNumber(
            firstRequestId,
            "%d품-국비04-01".formatted(proposalDate.getYear())
        );
        assertProposalNumber(
            secondRequestId,
            "%d품-국비04-02".formatted(proposalDate.getYear())
        );
    }

    @Test
    @DisplayName("반려된 신청은 품의번호 계산에서 제외한다")
    void proposalNumber_excludesRejectedRequest() {
        Long rejectedRequestId = createPurchaseRequest("반려할 이전 품의");
        Long activeRequestId = createPurchaseRequest("유효한 이후 품의");
        LocalDate activeDate = LocalDate.now();

        saveMinimalProposal(rejectedRequestId, activeDate.minusDays(1));
        saveMinimalProposal(activeRequestId, activeDate);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "테스트 반려"))
            .patch("/{requestId}/reject", rejectedRequestId)
            .then()
            .statusCode(200)
            .body("proposal.proposalNumber", nullValue());

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("overview", "반려 후 수정 시도"))
            .put("/{requestId}/proposal", rejectedRequestId)
            .then()
            .statusCode(409)
            .body("code", equalTo("PR-018"));

        assertProposalNumber(
            activeRequestId,
            "%d품-국비04-01".formatted(activeDate.getYear())
        );
    }

    @Test
    @DisplayName("소프트 삭제된 신청은 품의번호 계산에서 제외한다")
    void proposalNumber_excludesSoftDeletedRequest() {
        Long deletedRequestId = createPurchaseRequest("삭제할 이전 품의");
        Long activeRequestId = createPurchaseRequest("유효한 이후 품의");
        LocalDate activeDate = LocalDate.now();

        saveMinimalProposal(deletedRequestId, activeDate.minusDays(1));
        saveMinimalProposal(activeRequestId, activeDate);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .delete("/{requestId}", deletedRequestId)
            .then()
            .statusCode(204);

        assertProposalNumber(
            activeRequestId,
            "%d품-국비04-01".formatted(activeDate.getYear())
        );
    }

    @Test
    @DisplayName("직접 입력 선택 없이 직접 입력값을 보내면 400을 반환한다")
    void saveProposal_withInvalidCustomValue_returns400() {
        Long requestId = createPurchaseRequest("직접 입력 검증 테스트");

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("budget", Map.of(
                "itemCategory", "TEXTBOOK",
                "customItemCategory", "임의 항목"
            )))
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("미래 품의일자는 400을 반환한다")
    void saveProposal_withFutureDate_returns400() {
        Long requestId = createPurchaseRequest("미래 날짜 검증 테스트");

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("proposalDate", LocalDate.now().plusDays(1).toString()))
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("다른 작성자의 품의 정보는 수정할 수 없다")
    void saveProposal_asAnotherAuthor_returns403() {
        Long requestId = createPurchaseRequest("작성자 권한 테스트");

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteer2Token))
            .contentType(ContentType.JSON)
            .body(Map.of("overview", "다른 작성자의 수정"))
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(403)
            .body("code", equalTo("PR-002"));
    }

    @Test
    @DisplayName("관리자는 다른 작성자의 품의 정보를 저장할 수 있다")
    void saveProposal_asAdmin_returns200() {
        Long requestId = createPurchaseRequest("관리자 권한 테스트");

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("overview", "관리자 작성 품의"))
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(200)
            .body("overview", equalTo("관리자 작성 품의"));
    }

    @Test
    @DisplayName("작성자는 APPROVED와 PURCHASED 상태에서도 품의 정보를 수정할 수 있다")
    void saveProposal_beforeConfirmed_allowsApprovedAndPurchasedStatus() {
        Long requestId = createPurchaseRequest("최종 승인 전 수정 테스트");

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "테스트 승인"))
            .patch("/{requestId}/approve", requestId)
            .then()
            .statusCode(200);

        saveProposal(requestId, Map.of("overview", "승인 후 수정"));

        transactionTemplate.executeWithoutResult(status ->
            purchaseRequestRepository.findById(requestId).orElseThrow().reportPurchase()
        );

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("overview", "구매 완료 후 수정"))
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(200)
            .body("overview", equalTo("구매 완료 후 수정"));
    }

    @Test
    @DisplayName("CONFIRMED 상태에서는 품의 정보를 수정할 수 없다")
    void saveProposal_whenConfirmed_returns409() {
        Long requestId = createPurchaseRequest("최종 승인 상태 테스트");
        var purchaseRequest = purchaseRequestRepository.findById(requestId).orElseThrow();
        purchaseRequest.confirm();
        purchaseRequestRepository.saveAndFlush(purchaseRequest);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("overview", "수정 불가"))
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(409)
            .body("code", equalTo("PR-018"));
    }

    private Long createPurchaseRequest(String title) {
        Long requestId = given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", title),
                entry("classroomId", CLASSROOM_ID),
                entry("paymentType", "PREPAID"),
                entry("items", List.of(Map.ofEntries(
                    entry("name", "교재"),
                    entry("quantity", 1)
                )))
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
        createdRequestIds.add(requestId);
        return requestId;
    }

    private void saveMinimalProposal(Long requestId, LocalDate proposalDate) {
        saveMinimalProposal(requestId, proposalDate, "NATIONAL_SUBSIDY_04");
    }

    private void saveMinimalProposal(Long requestId, LocalDate proposalDate, String paymentAccount) {
        saveProposal(requestId, Map.of(
            "proposalDate", proposalDate.toString(),
            "paymentAccount", paymentAccount
        ));
    }

    private void saveProposal(Long requestId, Map<String, Object> body) {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(body)
            .put("/{requestId}/proposal", requestId)
            .then()
            .statusCode(200);
    }

    private void assertProposalNumber(Long requestId, String expectedProposalNumber) {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}", requestId)
            .then()
            .statusCode(200)
            .body("proposal.proposalNumber", equalTo(expectedProposalNumber));
    }

    private Map<String, Object> fullProposalBody(LocalDate proposalDate) {
        return Map.ofEntries(
            entry("overview", "교재 구입 품의"),
            entry("policyProject", "%d년 성인문해교육 지원사업".formatted(proposalDate.getYear())),
            entry("unitProject", "프로그램운영비"),
            entry("detailProject", "교재비"),
            entry("requestDepartmentId", 2L),
            entry("proposalDate", proposalDate.toString()),
            entry("proposalAmount", 2_000L),
            entry("paymentAccount", "NATIONAL_SUBSIDY_04"),
            entry("budget", Map.of(
                "itemCategory", "TEXTBOOK",
                "calculationDetail", "COMMERCIAL_TEXTBOOK"
            )),
            entry("items", List.of(Map.ofEntries(
                entry("content", "국어 교재"),
                entry("specification", "권"),
                entry("quantity", 1),
                entry("estimatedUnitPrice", 2_000L)
            )))
        );
    }

    private UUID uploadPurchaseReceipt(String fileName) {
        UUID fileId = UUID.fromString(given()
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .multiPart("file", fileName, "receipt".getBytes(), "image/png")
            .post("/api/v1/files/images/purchase-items")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("fileId"));
        uploadedFileIds.add(fileId);
        return fileId;
    }
}
