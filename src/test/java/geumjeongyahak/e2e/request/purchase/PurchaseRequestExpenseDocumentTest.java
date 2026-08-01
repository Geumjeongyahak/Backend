package geumjeongyahak.e2e.request.purchase;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
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
        if (createdVendorId != null && vendorRepository.existsById(createdVendorId)) {
            vendorRepository.deleteById(createdVendorId);
        }
    }

    @Test
    @DisplayName("작성자는 미완성 PENDING 품의서를 단독 DOCX로 생성할 수 있다")
    void generateProposalDocument_withIncompletePendingRequest_returnsProposalOnly() throws Exception {
        createdRequestId = createPrepaidPurchaseRequest();

        byte[] docx = requestProposalDocument(createdRequestId, volunteerToken)
            .then()
            .statusCode(200)
            .contentType(DOCX_CONTENT_TYPE)
            .extract()
            .asByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(fullDocumentText(document))
                .contains("지출 품의서")
                .doesNotContain("지출 결의서");
            assertThat(document.getTables()).hasSize(5);
        }
    }

    @Test
    @DisplayName("품의서에는 요청 바디보다 서버에 저장된 최신 품의 정보를 반영한다")
    void generateProposalDocument_usesStoredProposalData() throws Exception {
        createdRequestId = createPrepaidPurchaseRequest();
        LocalDate proposalDate = LocalDate.now();

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.ofEntries(
                entry("overview", "서버 저장 품의 개요"),
                entry("policyProject", "서버 정책 사업"),
                entry("unitProject", "서버 단위 사업"),
                entry("detailProject", "서버 세부 사업"),
                entry("proposalDate", proposalDate.toString()),
                entry("proposalAmount", 10000L),
                entry("paymentAccount", "NATIONAL_SUBSIDY_04"),
                entry("budget", Map.of(
                    "itemCategory", "TEXTBOOK",
                    "calculationDetail", "COMMERCIAL_TEXTBOOK"
                )),
                entry("items", List.of(Map.of(
                    "content", "서버 저장 품목",
                    "specification", "A4",
                    "quantity", 2,
                    "estimatedUnitPrice", 5000L
                )))
            ))
            .put("/{requestId}/proposal", createdRequestId)
            .then()
            .statusCode(200);

        byte[] docx = requestProposalDocument(createdRequestId, volunteerToken)
            .then()
            .statusCode(200)
            .extract()
            .asByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(fullDocumentText(document))
                .contains(
                    "서버 저장 품의 개요",
                    "서버 정책 사업",
                    "서버 단위 사업",
                    "서버 세부 사업",
                    "서버 저장 품목",
                    "교재비",
                    "시중교재",
                    "10,000원",
                    "%d품-국비04-01".formatted(proposalDate.getYear())
                )
                .doesNotContain("지출 결의서");
        }
    }

    @Test
    @DisplayName("품의 단계의 활성 영수증을 품의서에 첨부한다")
    void generateProposalDocument_withProposalReceipt_containsPicture() throws Exception {
        createdRequestId = createPrepaidPurchaseRequest();
        String receiptFileId = uploadPurchaseReceipt(PNG_BYTES);

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("fileId", receiptFileId))
            .post("/{requestId}/proposal/receipts", createdRequestId)
            .then()
            .statusCode(201);

        byte[] docx = requestProposalDocument(createdRequestId, volunteerToken)
            .then()
            .statusCode(200)
            .extract()
            .asByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getAllPictures()).hasSize(1);
        }
    }

    @Test
    @DisplayName("다른 작성자는 품의서를 생성할 수 없다")
    void generateProposalDocument_asOtherVolunteer_returns403() {
        createdRequestId = createPrepaidPurchaseRequest();

        requestProposalDocument(createdRequestId, volunteer2Token)
            .then()
            .statusCode(403)
            .body("code", org.hamcrest.Matchers.equalTo("PR-002"));
    }

    @Test
    @DisplayName("실 결제 요청은 품의서를 생성할 수 없다")
    void generateProposalDocument_withActualRequest_returns409() {
        createdRequestId = createPurchaseRequest("ACTUAL");

        requestProposalDocument(createdRequestId, volunteerToken)
            .then()
            .statusCode(409)
            .body("code", org.hamcrest.Matchers.equalTo("PR-011"));
    }

    @Test
    @DisplayName("최종 확인 전에는 결의서를 생성할 수 없다")
    void generateResolutionDocument_beforeConfirmation_returns409() {
        createdVendorId = createVendor();
        createdRequestId = createPurchasedPrepaidRequest();

        requestResolutionDocument(createdRequestId)
            .then()
            .statusCode(409)
            .body("code", org.hamcrest.Matchers.equalTo("PR-025"));
    }

    @Test
    @DisplayName("최종 확인된 실 결제 요청도 결의서를 생성할 수 없다")
    void generateResolutionDocument_withConfirmedActualRequest_returns409() {
        createdVendorId = createVendor();
        createdRequestId = createPurchaseRequest("ACTUAL");
        approvePurchaseRequest(createdRequestId);
        reportPurchase(createdRequestId, List.of(
            transaction(createdVendorId, 10000L, List.of("거래품목"), null)
        ));
        confirmPurchaseRequest(createdRequestId);

        requestResolutionDocument(createdRequestId)
            .then()
            .statusCode(409)
            .body("code", org.hamcrest.Matchers.equalTo("PR-011"));
    }

    @Test
    @DisplayName("결의서는 저장된 품의·거래 정보와 문서 입력 제목·완료 요청일을 사용한다")
    void generateResolutionDocument_usesDerivedDocumentValues() throws Exception {
        createdVendorId = createVendor();
        createdRequestId = createConfirmedPrepaidRequest(
            "TRANSFER",
            Map.ofEntries(
                entry("resolutionTitle", "문서 전용 결의 제목"),
                entry("completionDate", "2026-07-30"),
                entry("resolutionApprovals", List.of(
                    approvalLine("총무", "관리자"),
                    approvalLine("교장", "정해용")
                ))
            )
        );

        byte[] docx = requestResolutionDocument(createdRequestId)
            .then()
            .statusCode(200)
            .contentType(DOCX_CONTENT_TYPE)
            .extract()
            .asByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(fullDocumentText(document))
                .contains(
                    "지출 결의서",
                    "문서 전용 결의 제목",
                    "2026년 07월 30일",
                    "계좌이체",
                    "10,000원",
                    "총무",
                    "관리자"
                )
                .containsPattern("\\d{4}결-국비04-\\d{2}")
                .doesNotContain("지출 품의서", "{{resolutionAmount}}");
        }
        assertThat(documentXml(docx)).contains("■");
    }

    @Test
    @DisplayName("품의서 제목은 결제 신청 제목과 별도로 입력할 수 있다")
    void generateProposalDocument_withCustomTitle_usesInputTitle() throws Exception {
        createdRequestId = createPrepaidPurchaseRequest();

        saveProposal(createdRequestId, Map.of("proposalTitle", "문서 전용 품의 제목"))
            .then()
            .statusCode(200);

        Response response = requestProposalDocument(createdRequestId, volunteerToken)
            .then()
            .statusCode(200)
            .extract()
            .response();
        byte[] docx = response.asByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(fullDocumentText(document)).contains("문서 전용 품의 제목");
        }
        String contentDisposition = response.header("Content-Disposition");
        String encodedFilename = contentDisposition.substring(
            contentDisposition.indexOf("filename*=UTF-8''") + "filename*=UTF-8''".length()
        );
        assertThat(URLDecoder.decode(encodedFilename, StandardCharsets.UTF_8))
            .contains("품의서-문서 전용 품의 제목-");
    }

    @Test
    @DisplayName("결재·협조라인은 각각 최대 3명까지 입력할 수 있다")
    void generateProposalDocument_withFourApprovalLines_returns400() {
        createdRequestId = createPrepaidPurchaseRequest();
        List<Map<String, Object>> approvals = List.of(
            approvalLine("직위1", "이름1"),
            approvalLine("직위2", "이름2"),
            approvalLine("직위3", "이름3"),
            approvalLine("직위4", "이름4")
        );

        saveProposal(createdRequestId, Map.of("draftApprovals", approvals))
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("저장한 결재·협조라인을 품의서에 그대로 반영한다")
    void generateProposalDocument_usesStoredApprovalAndCooperationLines() throws Exception {
        createdRequestId = createPrepaidPurchaseRequest();

        saveProposal(createdRequestId, Map.of(
            "requestDepartmentId", 3L,
            "draftApprovals", List.of(approvalLine("총무", "결재자 입력값")),
            "draftCooperations", List.of(approvalLine("부서장", "협조자 입력값"))
        ))
            .then()
            .statusCode(200)
            .body("draftApprovals[0].position", org.hamcrest.Matchers.equalTo("총무"))
            .body("draftApprovals[0].name", org.hamcrest.Matchers.equalTo("결재자 입력값"))
            .body("draftCooperations[0].position", org.hamcrest.Matchers.equalTo("부서장"))
            .body("draftCooperations[0].name", org.hamcrest.Matchers.equalTo("협조자 입력값"));

        byte[] docx = requestProposalDocument(createdRequestId, volunteerToken)
            .then()
            .statusCode(200)
            .extract()
            .asByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(fullDocumentText(document))
                .contains("총무", "결재자 입력값", "부서장", "협조자 입력값");
        }
    }

    @Test
    @DisplayName("품의 품목이 5개를 초과해도 품의서를 생성할 수 있다")
    void generateProposalDocument_withMoreThanFiveItems_returnsDocx() throws Exception {
        createdRequestId = createPrepaidPurchaseRequest();
        List<Map<String, Object>> proposalItems = java.util.stream.IntStream.rangeClosed(1, 6)
            .mapToObj(index -> Map.<String, Object>of(
                "content", "품의 품목 " + index,
                "quantity", 1,
                "estimatedUnitPrice", 1000L
            ))
            .toList();

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of(
                "proposalDate", LocalDate.now().toString(),
                "proposalAmount", 6000L,
                "paymentAccount", "NATIONAL_SUBSIDY_04",
                "items", proposalItems
            ))
            .put("/{requestId}/proposal", createdRequestId)
            .then()
            .statusCode(200);

        byte[] docx = requestProposalDocument(createdRequestId, volunteerToken)
            .then()
            .statusCode(200)
            .extract()
            .asByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(fullDocumentText(document)).contains("품의 품목 1", "품의 품목 6");
        }
    }

    private Long createPurchasedPrepaidRequest() {
        Long requestId = createPrepaidPurchaseRequest();
        approvePurchaseRequest(requestId);
        reportPurchase(requestId, List.of(transaction(createdVendorId, 10000L, List.of("거래품목"), null)));
        return requestId;
    }

    private Long createConfirmedPrepaidRequest(String paymentMethod, Map<String, Object> documentFields) {
        Long requestId = createPrepaidPurchaseRequest();
        approvePurchaseRequest(requestId);
        reportPurchase(requestId, List.of(
            transaction(createdVendorId, 10000L, List.of("거래품목"), null, paymentMethod)
        ));
        confirmPurchaseRequest(requestId, documentFields);
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
                .map(this::purchaseItem)
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
                entry("paymentType", paymentType),
                entry("items", items)
            ))
            .post()
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    private Map<String, Object> purchaseItem(String name) {
        return purchaseItem(name, 1);
    }

    private Map<String, Object> purchaseItem(String name, int quantity) {
        return Map.ofEntries(
            entry("name", name),
            entry("reason", "문서 생성 테스트"),
            entry("quantity", quantity)
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
        confirmPurchaseRequest(requestId, Map.of("completionDate", LocalDate.now().toString()));
    }

    private void confirmPurchaseRequest(Long requestId, Map<String, Object> documentFields) {
        long totalPrice = given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}", requestId)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getLong("totalPrice");

        Map<String, Object> proposal = new java.util.LinkedHashMap<>(documentFields);
        proposal.put("proposalDate", LocalDate.now().toString());
        proposal.put("proposalAmount", totalPrice);
        proposal.put("paymentAccount", "NATIONAL_SUBSIDY_04");
        proposal.put("items", List.of(Map.of(
            "content", "지출증빙서류 테스트 품목",
            "quantity", 1,
            "estimatedUnitPrice", totalPrice
        )));

        saveProposal(requestId, proposal)
            .then()
            .statusCode(200);

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
        io.restassured.response.Response detail = given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .get("/{requestId}", requestId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        List<String> requestItemNames = detail.jsonPath().getList("items.name", String.class);
        String paymentType = detail.jsonPath().getString("paymentType");

        List<Map<String, Object>> normalizedTransactions = new java.util.ArrayList<>();
        if ("PREPAID".equals(paymentType)) {
            Map<String, Object> source = transactions.getFirst();
            Map<String, Object> transaction = new java.util.LinkedHashMap<>();
            transaction.put("vendorId", source.get("vendorId"));
            transaction.put("itemNames", requestItemNames);
            transaction.put(
                "amount",
                transactions.stream()
                    .mapToLong(value -> ((Number) value.get("amount")).longValue())
                    .sum()
            );
            transaction.put("paymentMethod", source.getOrDefault("paymentMethod", "CARD"));
            Object receiptFileId = transactions.stream()
                .map(value -> value.get("receiptFileId"))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseGet(() -> uploadPurchaseReceipt(PNG_BYTES));
            transaction.put("receiptFileId", receiptFileId);
            normalizedTransactions.add(transaction);
        } else {
            long totalAmount = transactions.stream()
                .mapToLong(value -> ((Number) value.get("amount")).longValue())
                .sum();
            long baseAmount = totalAmount / requestItemNames.size();
            long remainder = totalAmount % requestItemNames.size();
            for (int index = 0; index < requestItemNames.size(); index++) {
                Map<String, Object> source = transactions.get(Math.min(index, transactions.size() - 1));
                Map<String, Object> transaction = new java.util.LinkedHashMap<>();
                transaction.put("vendorId", source.get("vendorId"));
                transaction.put("itemNames", List.of(requestItemNames.get(index)));
                transaction.put(
                    "amount",
                    transactions.size() == requestItemNames.size()
                        ? source.get("amount")
                        : baseAmount + (index < remainder ? 1 : 0)
                );
                transaction.put("paymentMethod", source.getOrDefault("paymentMethod", "CARD"));
                if (source.get("receiptFileId") != null) {
                    transaction.put("receiptFileId", source.get("receiptFileId"));
                }
                normalizedTransactions.add(transaction);
            }
        }

        given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(Map.of("transactions", normalizedTransactions))
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
        return transaction(vendorId, amount, itemNames, receiptFileId, "CARD");
    }

    private Map<String, Object> transaction(
        Long vendorId,
        Long amount,
        List<String> itemNames,
        String receiptFileId,
        String paymentMethod
    ) {
        Map<String, Object> transaction = new java.util.LinkedHashMap<>();
        transaction.put("vendorId", vendorId);
        transaction.put("itemNames", itemNames);
        transaction.put("amount", amount);
        transaction.put("paymentMethod", paymentMethod);
        if (receiptFileId != null) {
            transaction.put("receiptFileId", receiptFileId);
        }
        return transaction;
    }

    private Response saveProposal(Long requestId, Map<String, Object> body) {
        return given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(volunteerToken))
            .contentType(ContentType.JSON)
            .body(body)
            .put("/{requestId}/proposal", requestId);
    }

    private Response requestProposalDocument(Long requestId, String accessToken) {
        return given()
            .basePath("/api/v1/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(accessToken))
            .post("/{requestId}/proposal-document", requestId);
    }

    private Response requestResolutionDocument(Long requestId) {
        return given()
            .basePath("/api/v1/admin/purchase-requests")
            .header(AUTH_HEADER, getAuthHeader(adminToken))
            .post("/{requestId}/resolution-document", requestId);
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

    private Map<String, Object> approvalLine(String position, String name) {
        return Map.of(
            "position", position,
            "name", name
        );
    }

}
