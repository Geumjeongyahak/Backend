package geumjeongyahak.e2e.request.purchase;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.domain.vendor.repository.VendorBalanceHistoryRepository;
import geumjeongyahak.domain.vendor.repository.VendorRepository;
import geumjeongyahak.e2e.request.RequestBaseTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@Tag("purchase-request")
@DisplayName("E2E: 지출증빙서류 생성 테스트")
class PurchaseRequestExpenseDocumentTest extends RequestBaseTest {

    private static final String DOCX_CONTENT_TYPE =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final byte[] PNG_BYTES = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
    );
    private static final byte[] PNG_BYTES_WITH_TRAILING_BYTE = appendTrailingByte(PNG_BYTES);

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorBalanceHistoryRepository vendorBalanceHistoryRepository;

    private Long createdRequestId;
    private Long createdVendorId;
    private final List<UUID> uploadedReceiptFileIds = new ArrayList<>();
    private UUID registeredReceiptFileId;

    @AfterEach
    void cleanup() {
        if (createdVendorId != null) {
            vendorBalanceHistoryRepository.deleteAllByVendor_Id(createdVendorId);
        }
        if (createdRequestId != null && purchaseRequestRepository.existsById(createdRequestId)) {
            purchaseRequestRepository.deleteById(createdRequestId);
        }
        for (UUID uploadedReceiptFileId : uploadedReceiptFileIds) {
            if (fileRepository.existsById(uploadedReceiptFileId)) {
                fileRepository.deleteById(uploadedReceiptFileId);
            }
        }
        if (registeredReceiptFileId != null && fileRepository.existsById(registeredReceiptFileId)) {
            fileRepository.deleteById(registeredReceiptFileId);
        }
        if (createdVendorId != null && vendorRepository.existsById(createdVendorId)) {
            vendorRepository.deleteById(createdVendorId);
        }
    }

    @Test
    @DisplayName("관리자가 결제 확인 완료된 선결제 구매 요청 지출증빙서류 DOCX를 생성할 수 있다")
    void generateExpenseDocument_withPrepaidPurchaseAndReceipt_returnsDocx() throws Exception {
        createdVendorId = createVendor();
        createdRequestId = createPrepaidPurchaseRequest();
        approvePurchaseRequest(createdRequestId);
        UUID uploadedReceiptFileId = UUID.fromString(uploadPurchaseReceipt(PNG_BYTES));
        reportPurchase(createdRequestId, List.of(
            transaction(createdVendorId, 1000L, List.of("거래품목1"), uploadedReceiptFileId.toString()),
            transaction(createdVendorId, 2000L, List.of("거래품목2"), null),
            transaction(createdVendorId, 3000L, List.of("거래품목3"), null),
            transaction(createdVendorId, 4000L, List.of("거래품목4"), null)
        ));
        confirmPurchaseRequest(createdRequestId);

        Response response = given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(expenseDocumentBody())
            .post("/{requestId}/expense-document", createdRequestId)
            .then()
            .statusCode(200)
            .contentType(DOCX_CONTENT_TYPE)
            .extract()
            .response();

        byte[] docx = response.asByteArray();
        assertThat(docx).isNotEmpty();
        assertThat(response.header("Content-Disposition"))
            .startsWith("attachment; filename=")
            .contains("filename*=UTF-8''")
            .contains("E2E")
            .contains(LocalDate.now().toString())
            .contains(".docx");

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            String documentText = document.getTables()
                .stream()
                .flatMap(table -> table.getRows().stream())
                .flatMap(row -> row.getTableCells().stream())
                .map(cell -> cell.getText())
                .reduce("", String::concat);

            assertThat(documentText)
                .contains(
                    "지출증빙서류 E2E",
                    "품목1",
                    "품목4",
                    "시중교재",
                    "규격4",
                    "4,000원",
                    "10,000원",
                    "거래품목4",
                    "카드결제",
                    "견적서 및 거래명세서(별첨)"
                )
                .doesNotContain("{{itemRows}}", "{{transactionRows}}", "[description]", "[detail]", "담당자", "연락처");
            assertThat(document.getTables().get(2).getRow(0).getTableCells())
                .extracting(cell -> cell.getText().replace("\n", ""))
                .containsExactly("순번", "내용", "규격", "예상단가", "수량", "예상금액");
            assertThat(document.getTables().get(1).getRows()).hasSize(3);
            assertThat(document.getTables().get(1).getRow(0).getTableCells())
                .extracting(cell -> cell.getText().replace("\n", ""))
                .containsExactly("순번", "세부사업", "세부항목", "산출내역", "품의금액", "예산잔액", "사업잔액");
            assertThat(document.getTables().get(2).getRows()).hasSize(6);
            assertThat(document.getTables().get(5).getRows()).hasSize(5);
            assertThat(document.getTables().get(5).getText()).doesNotContain("첨부서류", "비고");
            assertThat(document.getTables().get(7).getText()).contains("견적서 및 거래명세서(별첨)");
            assertThat(fullDocumentText(document)).contains("2026년 06월 30일");
            assertThat(document.getAllPictures()).hasSize(1);
        }
        assertThat(documentXml(docx)).contains("■");
    }

    @Test
    @DisplayName("품목 단가 계산 합계가 구매 완료 보고 금액과 다르면 지출증빙서류 생성에 실패한다")
    void generateExpenseDocument_withMismatchedItemAmountTotal_returns409() {
        createdVendorId = createVendor();
        createdRequestId = createPrepaidPurchaseRequest();
        approvePurchaseRequest(createdRequestId);
        reportPurchase(createdRequestId, List.of(transaction(createdVendorId, 10000L, List.of("거래품목"), null)));
        confirmPurchaseRequest(createdRequestId);

        requestExpenseDocument(createdRequestId, expenseDocumentBodyWithItems(List.of(
            expenseDocumentItem("예상규격1", 4000L),
            expenseDocumentItem("예상규격2", 5000L),
            expenseDocumentItem("예상규격3", 6000L),
            expenseDocumentItem("예상규격4", 7000L)
        )))
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("수동 확인용 지출증빙서류 샘플 DOCX를 생성한다")
    void generateExpenseDocumentSample() throws Exception {
        assumeTrue(
            isExpenseDocumentSampleGenerationEnabled(),
            "수동 샘플 생성이 필요할 때 GENERATE_EXPENSE_DOCUMENT_SAMPLE=true 환경변수로 실행합니다."
        );
        createdVendorId = createVendor();
        createdRequestId = createPurchaseRequestWithItems("PREPAID", List.of(
            purchaseItem("문해 교재 1단계", "PREPAID", 10),
            purchaseItem("문해 교재 2단계", "PREPAID", 10),
            purchaseItem("수업용 문제집", "PREPAID", 10)
        ));
        approvePurchaseRequest(createdRequestId);
        reportPurchase(createdRequestId, List.of(
            transaction(createdVendorId, 100000L, List.of("문해 교재 1단계", "문해 교재 2단계", "수업용 문제집"), null)
        ));
        confirmPurchaseRequest(createdRequestId);

        byte[] docx = requestExpenseDocument(createdRequestId, expenseDocumentSampleBody())
            .then()
            .statusCode(200)
            .contentType(DOCX_CONTENT_TYPE)
            .extract()
            .asByteArray();

        Path samplePath = Path.of("build", "generated-samples", "expense-document-sample.docx");
        Files.createDirectories(samplePath.getParent());
        Files.write(samplePath, docx);

        assertThat(Files.size(samplePath)).isGreaterThan(0);
    }

    @Test
    @DisplayName("CONFIRMED 상태의 선결제 구매 요청은 지출증빙서류 DOCX를 생성할 수 있다")
    void generateExpenseDocument_withConfirmedPurchase_returnsDocx() {
        createdVendorId = createVendor();
        createdRequestId = createConfirmedPrepaidRequest();

        requestExpenseDocument(createdRequestId, expenseDocumentBody())
            .then()
            .statusCode(200)
            .contentType(DOCX_CONTENT_TYPE);
    }

    @Test
    @DisplayName("존재하지 않는 구매 요청의 지출증빙서류 생성은 404를 반환한다")
    void generateExpenseDocument_withMissingPurchaseRequest_returns404() {
        requestExpenseDocument(999999L, expenseDocumentBody())
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("인증 없이 지출증빙서류를 생성할 수 없다")
    void generateExpenseDocument_withoutAuthentication_returns401() {
        given()
            .basePath("/api/v1/admin/purchase-requests")
            .contentType(ContentType.JSON)
            .body(expenseDocumentBody())
            .post("/{requestId}/expense-document", 999999L)
            .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("매니저 권한 사용자는 지출증빙서류를 생성할 수 없다")
    void generateExpenseDocument_withManagerPermission_returns403() {
        createdVendorId = createVendor();
        createdRequestId = createConfirmedPrepaidRequest();

        requestExpenseDocument(createdRequestId, expenseDocumentBody(), managerToken)
            .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("최소 요청 바디로도 지출증빙서류 DOCX를 생성할 수 있다")
    void generateExpenseDocument_withEmptyBody_returnsDocx() {
        createdVendorId = createVendor();
        createdRequestId = createConfirmedPrepaidRequest();

        requestExpenseDocument(createdRequestId, Map.of())
            .then()
            .statusCode(200)
            .contentType(DOCX_CONTENT_TYPE);
    }

    @Test
    @DisplayName("영수증이 2개 이상이면 지출증빙서류 DOCX에 모두 첨부된다")
    void generateExpenseDocument_withMultipleReceipts_containsAllPictures() throws Exception {
        createdVendorId = createVendor();
        createdRequestId = createPrepaidPurchaseRequest();
        approvePurchaseRequest(createdRequestId);
        UUID receiptFileId1 = UUID.fromString(uploadPurchaseReceipt(PNG_BYTES));
        UUID receiptFileId2 = UUID.fromString(uploadPurchaseReceipt(PNG_BYTES_WITH_TRAILING_BYTE));
        reportPurchase(createdRequestId, List.of(
            transaction(createdVendorId, 5000L, List.of("거래품목1"), receiptFileId1.toString()),
            transaction(createdVendorId, 5000L, List.of("거래품목2"), receiptFileId2.toString())
        ));
        confirmPurchaseRequest(createdRequestId);

        byte[] docx = requestExpenseDocument(createdRequestId, expenseDocumentBody())
            .then()
            .statusCode(200)
            .contentType(DOCX_CONTENT_TYPE)
            .extract()
            .asByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getAllPictures()).hasSize(2);
        }
    }

    @Test
    @DisplayName("영수증 메타데이터가 이미지여도 실제 바이트가 깨져 있으면 생성에 실패한다")
    void generateExpenseDocument_withBrokenReceiptImageBytes_returns409() {
        createdVendorId = createVendor();
        createdRequestId = createPrepaidPurchaseRequest();
        approvePurchaseRequest(createdRequestId);
        UUID receiptFileId = UUID.fromString(uploadPurchaseReceipt("not-an-image".getBytes()));
        reportPurchase(createdRequestId, List.of(
            transaction(createdVendorId, 10000L, List.of("거래품목"), receiptFileId.toString())
        ));
        confirmPurchaseRequest(createdRequestId);

        requestExpenseDocument(createdRequestId, expenseDocumentBody())
            .then()
            .statusCode(409);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CASH", "CARD", "TRANSFER", "AUTO_TRANSFER", "OTHER"})
    @DisplayName("모든 지급구분 값으로 지출증빙서류 DOCX를 생성할 수 있다")
    void generateExpenseDocument_withEveryPaymentMethod_returnsDocx(String paymentMethod) throws Exception {
        createdVendorId = createVendor();
        createdRequestId = createConfirmedPrepaidRequest();

        byte[] docx = requestExpenseDocument(createdRequestId, expenseDocumentBody(paymentMethod))
            .then()
            .statusCode(200)
            .contentType(DOCX_CONTENT_TYPE)
            .extract()
            .asByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(documentText(document)).contains("지출증빙서류 E2E", "10,000원");
        }
    }

    @Test
    @DisplayName("결재라인과 품목 보완값이 기준보다 많아도 초과 입력을 무시하고 생성한다")
    void generateExpenseDocument_withExcessApprovalLinesAndItemInputs_returnsDocx() throws Exception {
        createdVendorId = createVendor();
        createdRequestId = createConfirmedPrepaidRequest();

        byte[] docx = requestExpenseDocument(createdRequestId, expenseDocumentBodyWithItemsAndApprovalLines(
            List.of(
                expenseDocumentItem("초과규격1", 1000L),
                expenseDocumentItem("초과규격2", 2000L),
                expenseDocumentItem("초과규격3", 3000L),
                expenseDocumentItem("초과규격4", 4000L),
                expenseDocumentItem("무시규격5", 5000L),
                expenseDocumentItem("무시규격6", 6000L)
            ),
            List.of(
                approvalLine("직위1", "이름1"),
                approvalLine("직위2", "이름2"),
                approvalLine("직위3", "이름3"),
                approvalLine("직위4", "이름4"),
                approvalLine("직위5", "이름5"),
                approvalLine("직위6", "이름6")
            )
        ))
            .then()
            .statusCode(200)
            .contentType(DOCX_CONTENT_TYPE)
            .extract()
            .asByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(documentText(document))
                .contains("초과규격4", "직위5")
                .doesNotContain("무시규격5", "직위6");
        }
    }

    @Test
    @DisplayName("선결제가 아닌 구매 요청은 지출증빙서류를 생성할 수 없다")
    void generateExpenseDocument_withActualPaymentItem_returns409() {
        createdVendorId = createVendor();
        createdRequestId = createPurchaseRequest("ACTUAL");
        approvePurchaseRequest(createdRequestId);
        reportPurchase(createdRequestId, List.of(transaction(createdVendorId, 10000L, List.of("거래품목"), null)));
        confirmPurchaseRequest(createdRequestId);

        requestExpenseDocument(createdRequestId, expenseDocumentBody())
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("CONFIRMED가 아닌 상태에서는 지출증빙서류를 생성할 수 없다")
    void generateExpenseDocument_withoutConfirmedStatus_returns409() {
        createdRequestId = createPrepaidPurchaseRequest();

        requestExpenseDocument(createdRequestId, expenseDocumentBody())
            .then()
            .statusCode(409);

        approvePurchaseRequest(createdRequestId);

        requestExpenseDocument(createdRequestId, expenseDocumentBody())
            .then()
            .statusCode(409);

        createdVendorId = createVendor();
        reportPurchase(createdRequestId, List.of(transaction(createdVendorId, 10000L, List.of("거래품목"), null)));

        requestExpenseDocument(createdRequestId, expenseDocumentBody())
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("영수증이 없는 거래도 지출증빙서류 DOCX를 생성할 수 있다")
    void generateExpenseDocument_withoutReceipt_returnsDocxWithoutPictures() throws Exception {
        createdVendorId = createVendor();
        createdRequestId = createPrepaidPurchaseRequest();
        approvePurchaseRequest(createdRequestId);
        reportPurchase(createdRequestId, List.of(transaction(createdVendorId, 10000L, List.of("거래품목"), null)));
        confirmPurchaseRequest(createdRequestId);

        byte[] docx = requestExpenseDocument(createdRequestId, expenseDocumentBody())
            .then()
            .statusCode(200)
            .contentType(DOCX_CONTENT_TYPE)
            .extract()
            .asByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getAllPictures()).isEmpty();
        }
    }

    @Test
    @DisplayName("지원하지 않는 영수증 이미지 형식이면 지출증빙서류 생성에 실패한다")
    void generateExpenseDocument_withUnsupportedReceiptImage_returns409() {
        createdVendorId = createVendor();
        createdRequestId = createPrepaidPurchaseRequest();
        approvePurchaseRequest(createdRequestId);
        registeredReceiptFileId = UUID.fromString(registerDriveReceipt("application/pdf", "pdf"));
        reportPurchase(createdRequestId, List.of(
            transaction(createdVendorId, 10000L, List.of("거래품목"), registeredReceiptFileId.toString())
        ));
        confirmPurchaseRequest(createdRequestId);

        requestExpenseDocument(createdRequestId, expenseDocumentBody())
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("품목 단가 보완 입력값이 일부만 있으면 지출증빙서류 생성에 실패한다")
    void generateExpenseDocument_withPartialItemInputs_returns409() {
        createdVendorId = createVendor();
        createdRequestId = createPrepaidPurchaseRequest();
        approvePurchaseRequest(createdRequestId);
        reportPurchase(createdRequestId, List.of(transaction(createdVendorId, 10000L, List.of("거래품목"), null)));
        confirmPurchaseRequest(createdRequestId);

        requestExpenseDocument(createdRequestId, expenseDocumentBodyWithItems(List.of(
            expenseDocumentItem("부분규격1", 1000L),
            expenseDocumentItem("부분규격2", 2000L)
        )))
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("권한 없는 사용자는 지출증빙서류를 생성할 수 없다")
    void generateExpenseDocument_withoutAdminPermission_returns403() {
        createdVendorId = createVendor();
        createdRequestId = createPrepaidPurchaseRequest();
        approvePurchaseRequest(createdRequestId);
        reportPurchase(createdRequestId, List.of(transaction(createdVendorId, 10000L, List.of("거래품목"), null)));
        confirmPurchaseRequest(createdRequestId);

        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(expenseDocumentBody())
            .post("/{requestId}/expense-document", createdRequestId)
            .then()
            .statusCode(403);
    }

    private Long createPurchasedPrepaidRequest() {
        Long requestId = createPrepaidPurchaseRequest();
        approvePurchaseRequest(requestId);
        reportPurchase(requestId, List.of(transaction(createdVendorId, 10000L, List.of("거래품목"), null)));
        return requestId;
    }

    private Long createConfirmedPrepaidRequest() {
        Long requestId = createPurchasedPrepaidRequest();
        confirmPurchaseRequest(requestId);
        return requestId;
    }

    private Long createPrepaidPurchaseRequest() {
        return createPurchaseRequest("PREPAID");
    }

    private Long createPurchaseRequest(String paymentType) {
        return createPurchaseRequest(paymentType, List.of("품목1", "품목2", "품목3", "품목4"));
    }

    private Long createPurchaseRequest(String paymentType, List<String> itemNames) {
        return createPurchaseRequestWithItems(
            paymentType,
            itemNames.stream()
                .map(itemName -> purchaseItem(itemName, paymentType))
                .toList()
        );
    }

    private Long createPurchaseRequestWithItems(String paymentType, List<Map<String, Object>> items) {
        return given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("title", "지출증빙서류 E2E"),
                entry("content", "지출증빙서류 생성 API 테스트입니다."),
                entry("classroomId", CLASSROOM_ID),
                entry("items", items)
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    private Map<String, Object> purchaseItem(String name, String paymentType) {
        return purchaseItem(name, paymentType, 1);
    }

    private Map<String, Object> purchaseItem(String name, String paymentType, int quantity) {
        return Map.ofEntries(
            entry("name", name),
            entry("reason", "문서 생성 테스트"),
            entry("quantity", quantity),
            entry("paymentType", paymentType)
        );
    }

    private Long createVendor() {
        Long vendorId = given()
            .basePath("/api/v1/admin/vendors")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("name", "지출증빙서류 테스트 거래처"))
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
            .body(Map.of("amount", 100000L, "memo", "지출증빙서류 테스트 충전"))
            .post("/{vendorId}/charges", vendorId)
            .then()
            .statusCode(200);

        return vendorId;
    }

    private void approvePurchaseRequest(Long requestId) {
        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of("note", "지출증빙서류 테스트 승인"))
            .patch("/{requestId}/approve", requestId)
            .then()
            .statusCode(200);
    }

    private void confirmPurchaseRequest(Long requestId) {
        given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .patch("/{requestId}/confirm", requestId)
            .then()
            .statusCode(200);
    }

    private String uploadPurchaseReceipt(byte[] content) {
        String fileId = given()
            .basePath("/api/v1/files")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.MULTIPART)
            .multiPart("file", "receipt.png", content, "image/png")
            .post("/images/purchase-items")
            .then()
            .statusCode(201)
            .extract()
            .path("fileId");
        uploadedReceiptFileIds.add(UUID.fromString(fileId));
        return fileId;
    }

    private void reportPurchase(Long requestId, List<Map<String, Object>> transactions) {
        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", transactions))
            .post("/{requestId}/report", requestId)
            .then()
            .statusCode(200);
    }

    private Map<String, Object> transaction(
        Long vendorId,
        Long amount,
        List<String> itemNames,
        String receiptFileId
    ) {
        Map<String, Object> transaction = new java.util.LinkedHashMap<>();
        transaction.put("vendorId", vendorId);
        transaction.put("itemNames", itemNames);
        transaction.put("amount", amount);
        if (receiptFileId != null) {
            transaction.put("receiptFileId", receiptFileId);
        }
        return transaction;
    }

    private Response requestExpenseDocument(Long requestId, Map<String, Object> body) {
        return requestExpenseDocument(requestId, body, adminToken);
    }

    private Response requestExpenseDocument(Long requestId, Map<String, Object> body, String accessToken) {
        return given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(accessToken))
            .contentType(ContentType.JSON)
            .body(body)
            .post("/{requestId}/expense-document", requestId);
    }

    private String registerDriveReceipt(String mimeType, String ext) {
        return given()
            .basePath("/api/v1/files")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "driveUrl", "https://drive.google.com/file/d/unsupported-receipt/view?usp=sharing",
                "originalName", "unsupported-receipt." + ext,
                "mimeType", mimeType,
                "fileSize", 1024L
            ))
            .post("/drive")
            .then()
            .statusCode(201)
            .extract()
            .path("fileId");
    }

    private Map<String, Object> expenseDocumentBody() {
        return expenseDocumentBody("CARD");
    }

    private Map<String, Object> expenseDocumentBody(String paymentMethod) {
        return expenseDocumentBodyWithItems(List.of(
            expenseDocumentItem("규격1", 1000L),
            expenseDocumentItem("규격2", 2000L),
            expenseDocumentItem("규격3", 3000L),
            expenseDocumentItem("규격4", 4000L)
        ), paymentMethod);
    }

    private Map<String, Object> expenseDocumentBodyWithItems(List<Map<String, Object>> items) {
        return expenseDocumentBodyWithItems(items, "CARD");
    }

    private Map<String, Object> expenseDocumentBodyWithItems(List<Map<String, Object>> items, String paymentMethod) {
        return expenseDocumentBodyWithItemsAndApprovalLines(
            items,
            paymentMethod,
            List.of(approvalLine("담당", "김담당")),
            List.of(approvalLine("협조", "이협조")),
            List.of(approvalLine("확인", "박확인"))
        );
    }

    private Map<String, Object> expenseDocumentBodyWithItemsAndApprovalLines(
        List<Map<String, Object>> items,
        List<Map<String, Object>> approvalLines
    ) {
        return expenseDocumentBodyWithItemsAndApprovalLines(
            items,
            "CARD",
            approvalLines,
            approvalLines,
            approvalLines
        );
    }

    private Map<String, Object> expenseDocumentBodyWithItemsAndApprovalLines(
        List<Map<String, Object>> items,
        String paymentMethod,
        List<Map<String, Object>> draftApprovals,
        List<Map<String, Object>> draftCooperations,
        List<Map<String, Object>> resolutionApprovals
    ) {
        return Map.ofEntries(
            entry("fiscalYear", "2026년"),
            entry("draftDocumentNumber", "2026품-테스트-01"),
            entry("resolutionDocumentNumber", "2026결-테스트-01"),
            entry("policyProject", "테스트 정책사업"),
            entry("unitProject", "테스트 단위사업"),
            entry("detailProject", "테스트 세부사업"),
            entry("budgetDetail", "시중교재"),
            entry("budgetBalance", 100000L),
            entry("projectBalance", 500000L),
            entry("requestDepartment", "교육연구부"),
            entry("draftDate", "2026. 06. 30."),
            entry("completionDate", "2026. 06. 30."),
            entry("receiver", "금정열린배움터"),
            entry("paymentMethod", paymentMethod),
            entry("initiationDate", "2026. 06. 30."),
            entry("resolutionDate", "2026. 06. 30."),
            entry("paymentDate", "2026. 06. 30."),
            entry("items", items),
            entry("draftApprovals", draftApprovals),
            entry("draftCooperations", draftCooperations),
            entry("resolutionApprovals", resolutionApprovals)
        );
    }

    private Map<String, Object> expenseDocumentSampleBody() {
        return expenseDocumentBodyWithItemsAndApprovalLines(
            List.of(
                expenseDocumentItem("A4", 3000L),
                expenseDocumentItem("A4", 3000L),
                expenseDocumentItem("A4", 4000L)
            ),
            "TRANSFER",
            List.of(
                approvalLine("재정", "김재정"),
                approvalLine("대표", "오대표")
            ),
            List.of(
                approvalLine("교육", "이교육")
            ),
            List.of(
                approvalLine("담당", "박담당"),
                approvalLine("회계", "최회계")
            )
        );
    }

    private String documentText(XWPFDocument document) {
        return document.getTables()
            .stream()
            .flatMap(table -> table.getRows().stream())
            .flatMap(row -> row.getTableCells().stream())
            .map(cell -> cell.getText())
            .reduce("", String::concat);
    }

    private String fullDocumentText(XWPFDocument document) {
        String paragraphText = document.getParagraphs()
            .stream()
            .map(paragraph -> paragraph.getText())
            .reduce("", String::concat);
        return paragraphText + documentText(document);
    }

    private String documentXml(byte[] docx) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    return new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IOException("word/document.xml not found");
    }

    private Map<String, Object> expenseDocumentItem(String spec, Long unitPrice) {
        return Map.of(
            "spec", spec,
            "unitPrice", unitPrice
        );
    }

    private Map<String, Object> approvalLine(String position, String name) {
        return Map.of(
            "position", position,
            "name", name
        );
    }

    private boolean isExpenseDocumentSampleGenerationEnabled() {
        return Boolean.getBoolean("generateExpenseDocumentSample")
            || Boolean.parseBoolean(System.getenv("GENERATE_EXPENSE_DOCUMENT_SAMPLE"));
    }

    private static byte[] appendTrailingByte(byte[] source) {
        byte[] copy = java.util.Arrays.copyOf(source, source.length + 1);
        copy[source.length] = 0;
        return copy;
    }
}
