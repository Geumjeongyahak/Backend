package geumjeongyahak.domain.purchase_request.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import com.deepoove.poi.XWPFTemplate;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestItem;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestPaymentTransaction;
import geumjeongyahak.domain.purchase_request.enums.ExpenseDocumentPaymentMethod;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentType;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.exception.PurchaseRequestErrorCode;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.domain.purchase_request.v1.dto.request.GenerateExpenseDocumentRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.GenerateExpenseDocumentRequest.ApprovalLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseDocumentService {

    public static final String DOCX_CONTENT_TYPE =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String TEMPLATE_PATH = "templates/docx/expense-document-template.docx";
    private static final String CHECKED = "☑";
    private static final String UNCHECKED = "□";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy. MM. dd.");
    private static final NumberFormat MONEY_FORMATTER = NumberFormat.getNumberInstance(Locale.KOREA);
    private static final int TEMPLATE_ITEM_ROW_COUNT = 3;
    private static final int TEMPLATE_TRANSACTION_ROW_COUNT = 3;
    private static final int TEMPLATE_APPROVAL_SLOT_COUNT = 9;
    private static final int TEMPLATE_PLACEHOLDER_START_INDEX = 1;

    private final PurchaseRequestRepository purchaseRequestRepository;

    public byte[] generate(Long purchaseRequestId, GenerateExpenseDocumentRequest request) {
        log.debug("지출증빙서류 생성 요청 (purchaseRequestId={})", purchaseRequestId);
        PurchaseRequest purchaseRequest = findPurchaseRequest(purchaseRequestId);
        validateGeneratable(purchaseRequest);

        return renderTemplate(buildRenderData(purchaseRequest, request));
    }

    private byte[] loadTemplate() {
        ClassPathResource template = new ClassPathResource(TEMPLATE_PATH);
        if (!template.exists()) {
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_TEMPLATE_NOT_FOUND);
        }

        try {
            return StreamUtils.copyToByteArray(template.getInputStream());
        } catch (IOException e) {
            log.error("지출증빙서류 템플릿 로드 실패 (path={})", TEMPLATE_PATH, e);
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_TEMPLATE_READ_FAILED);
        }
    }

    private byte[] renderTemplate(Map<String, Object> data) {
        try (
            ByteArrayInputStream templateInputStream = new ByteArrayInputStream(loadTemplate());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            XWPFTemplate template = XWPFTemplate.compile(templateInputStream).render(data)
        ) {
            template.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException | RuntimeException e) {
            log.error("지출증빙서류 DOCX 렌더링 실패", e);
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_GENERATION_FAILED);
        }
    }

    private PurchaseRequest findPurchaseRequest(Long purchaseRequestId) {
        return purchaseRequestRepository.findById(purchaseRequestId)
            .orElseThrow(() -> new ResourceNotFoundException(PurchaseRequestErrorCode.NOT_FOUND, purchaseRequestId));
    }

    private void validateGeneratable(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getStatus() != PurchaseRequestStatus.PURCHASED
            && purchaseRequest.getStatus() != PurchaseRequestStatus.CONFIRMED) {
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_UNSUPPORTED_STATUS);
        }

        boolean allPrepaid = purchaseRequest.getItems().stream()
            .map(PurchaseRequestItem::getPaymentType)
            .allMatch(PurchasePaymentType.PREPAID::equals);
        if (!allPrepaid) {
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_ONLY_PREPAID_ALLOWED);
        }
    }

    private Map<String, Object> buildRenderData(
        PurchaseRequest purchaseRequest,
        GenerateExpenseDocumentRequest request
    ) {
        Map<String, Object> data = new HashMap<>();
        fillAllKnownPlaceholders(data);

        String amount = formatMoney(purchaseRequest.getTotalPrice());
        String purchasedAt = formatDate(purchaseRequest.getPurchasedAt());
        String draftDate = defaultText(request.draftDate(), formatDate(purchaseRequest.getCreatedAt()));
        String paymentDate = defaultText(request.paymentDate(), purchasedAt);
        String resolutionDate = defaultText(request.resolutionDate(), purchasedAt);
        String initiationDate = defaultText(request.initiationDate(), resolutionDate);
        String overview = defaultText(purchaseRequest.getContent(), "아래 비용을 다음과 같이 지출하고자 합니다.");

        data.put("fiscalYear", defaultText(request.fiscalYear(), ""));
        data.put("draftDocumentNumber", defaultText(request.draftDocumentNumber(), ""));
        data.put("resolutionDocumentNumber", defaultText(request.resolutionDocumentNumber(), ""));
        data.put("draftTitle", purchaseRequest.getTitle());
        data.put("draftOverview", overview);
        data.put("draftCostSummary", "1. 청구비용 : " + amount);
        data.put("policyProject", defaultText(request.policyProject(), ""));
        data.put("requestDepartment", defaultText(request.requestDepartment(), ""));
        data.put("unitProject", defaultText(request.unitProject(), ""));
        data.put("draftDate", draftDate);
        data.put("detailProject", defaultText(request.detailProject(), ""));
        data.put("draftAmount", amount);
        data.put("budgetNo1", "1");
        data.put("budgetProject1", defaultText(request.detailProject(), ""));
        data.put("budgetItem1", defaultText(request.unitProject(), ""));
        data.put("budgetDetail1", summarizeItemNames(purchaseRequest.getItems()));
        data.put("budgetAmount1", amount);
        data.put("budgetTotal", amount);
        data.put("completionDate", defaultText(request.completionDate(), ""));
        data.put("requesterName", purchaseRequest.getRequestedBy().getName());
        data.put("receiver", defaultText(request.receiver(), ""));
        data.put("draftNote", defaultText(request.note(), ""));

        fillItems(data, purchaseRequest.getItems());

        data.put("initiationDate", initiationDate);
        data.put("resolutionDate", resolutionDate);
        data.put("resolutionTitle", purchaseRequest.getTitle());
        data.put("resolutionAmount", amount);
        data.put("resolutionContent", purchaseRequest.getContent());
        data.put("attachments", "영수증");
        data.put("resolutionNote", defaultText(request.note(), ""));
        data.put("transactionTotal", amount);
        data.put("paymentDate", paymentDate);
        data.put("bankAccount", defaultText(request.bankAccount(), ""));
        data.put("vendorName", summarizeVendors(purchaseRequest.getTransactions()));
        data.put("businessNumber", defaultText(request.businessNumber(), ""));
        data.put("paymentAmount", amount);
        data.put("accountHolder", defaultText(request.accountHolder(), ""));
        data.put("manager", defaultText(request.manager(), ""));
        data.put("contact", defaultText(request.contact(), ""));

        fillPaymentMethod(data, request.paymentMethod());
        fillTransactions(data, purchaseRequest.getTransactions(), paymentDate);
        fillApprovalLines(data, "draftApproval", request.draftApprovals());
        fillApprovalLines(data, "draftCooperation", request.draftCooperations());
        fillApprovalLines(data, "resolutionApproval", request.resolutionApprovals());

        return data;
    }

    private void fillAllKnownPlaceholders(Map<String, Object> data) {
        List.of(
            "fiscalYear",
            "draftDocumentNumber",
            "resolutionDocumentNumber",
            "draftTitle",
            "draftOverview",
            "draftCostSummary",
            "policyProject",
            "requestDepartment",
            "unitProject",
            "draftDate",
            "detailProject",
            "draftAmount",
            "budgetNo1",
            "budgetProject1",
            "budgetItem1",
            "budgetDetail1",
            "budgetAmount1",
            "budgetBalance1",
            "budgetNo2",
            "budgetProject2",
            "budgetItem2",
            "budgetDetail2",
            "budgetAmount2",
            "budgetBalance2",
            "budgetTotal",
            "budgetTotalBalance",
            "itemTotalQuantity",
            "itemTotalAmount",
            "completionDate",
            "requesterName",
            "receiver",
            "draftNote",
            "payCash",
            "payCard",
            "payTransfer",
            "payAuto",
            "payOther",
            "initiationDate",
            "resolutionDate",
            "resolutionTitle",
            "resolutionAmount",
            "resolutionContent",
            "attachments",
            "resolutionNote",
            "transactionTotal",
            "transactionTotalNote",
            "attachmentSpace",
            "paymentDate",
            "bankAccount",
            "vendorName",
            "businessNumber",
            "paymentAmount",
            "accountHolder",
            "manager",
            "contact"
        ).forEach(key -> data.put(key, ""));

        for (int i = TEMPLATE_PLACEHOLDER_START_INDEX; i <= TEMPLATE_ITEM_ROW_COUNT; i++) {
            data.put("itemNo" + i, "");
            data.put("itemDescription" + i, "");
            data.put("itemSpec" + i, "");
            data.put("itemQuantity" + i, "");
            data.put("itemUnitPrice" + i, "");
            data.put("itemAmount" + i, "");
        }

        for (int i = TEMPLATE_PLACEHOLDER_START_INDEX; i <= TEMPLATE_TRANSACTION_ROW_COUNT; i++) {
            data.put("transactionNo" + i, "");
            data.put("transactionDate" + i, "");
            data.put("transactionDetail" + i, "");
            data.put("transactionAmount" + i, "");
            data.put("transactionNote" + i, "");
        }

        for (int i = TEMPLATE_PLACEHOLDER_START_INDEX; i <= TEMPLATE_APPROVAL_SLOT_COUNT; i++) {
            data.put("draftApproval" + i, "");
            data.put("draftCooperation" + i, "");
            data.put("resolutionApproval" + i, "");
        }
    }

    private void fillItems(Map<String, Object> data, List<PurchaseRequestItem> items) {
        for (int index = 0; index < Math.min(items.size(), TEMPLATE_ITEM_ROW_COUNT); index++) {
            PurchaseRequestItem item = items.get(index);
            int row = index + TEMPLATE_PLACEHOLDER_START_INDEX;
            data.put("itemNo" + row, String.valueOf(row));
            data.put("itemDescription" + row, item.getName());
            data.put("itemQuantity" + row, String.valueOf(item.getQuantity()));
        }
        data.put("itemTotalQuantity", String.valueOf(items.stream().mapToInt(PurchaseRequestItem::getQuantity).sum()));
    }

    private void fillTransactions(
        Map<String, Object> data,
        List<PurchaseRequestPaymentTransaction> transactions,
        String paymentDate
    ) {
        for (int index = 0; index < Math.min(transactions.size(), TEMPLATE_TRANSACTION_ROW_COUNT); index++) {
            PurchaseRequestPaymentTransaction transaction = transactions.get(index);
            int row = index + TEMPLATE_PLACEHOLDER_START_INDEX;
            data.put("transactionNo" + row, String.valueOf(row));
            data.put("transactionDate" + row, paymentDate);
            data.put("transactionDetail" + row, String.join(", ", transaction.getItemNames()));
            data.put("transactionAmount" + row, formatMoney(transaction.getAmount()));
            data.put("transactionNote" + row, transaction.getVendor().getName());
        }
    }

    private void fillPaymentMethod(Map<String, Object> data, ExpenseDocumentPaymentMethod paymentMethod) {
        data.put("payCash", paymentMethod == ExpenseDocumentPaymentMethod.CASH ? CHECKED : UNCHECKED);
        data.put("payCard", paymentMethod == ExpenseDocumentPaymentMethod.CARD ? CHECKED : UNCHECKED);
        data.put("payTransfer", paymentMethod == ExpenseDocumentPaymentMethod.TRANSFER ? CHECKED : UNCHECKED);
        data.put("payAuto", paymentMethod == ExpenseDocumentPaymentMethod.AUTO_TRANSFER ? CHECKED : UNCHECKED);
        data.put("payOther", paymentMethod == ExpenseDocumentPaymentMethod.OTHER ? CHECKED : UNCHECKED);
    }

    private void fillApprovalLines(Map<String, Object> data, String keyPrefix, List<ApprovalLine> approvalLines) {
        if (approvalLines == null || approvalLines.isEmpty()) {
            return;
        }

        int keyIndex = TEMPLATE_PLACEHOLDER_START_INDEX;
        for (ApprovalLine line : approvalLines) {
            if (keyIndex > TEMPLATE_APPROVAL_SLOT_COUNT) {
                return;
            }
            data.put(keyPrefix + keyIndex, defaultText(line.position(), ""));
            keyIndex++;

            if (keyIndex > TEMPLATE_APPROVAL_SLOT_COUNT) {
                return;
            }
            data.put(keyPrefix + keyIndex, defaultText(line.name(), ""));
            keyIndex++;
        }
    }

    private String summarizeItemNames(List<PurchaseRequestItem> items) {
        if (items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.getFirst().getName();
        }
        return items.getFirst().getName() + " 외 " + (items.size() - 1) + "건";
    }

    private String summarizeVendors(List<PurchaseRequestPaymentTransaction> transactions) {
        List<String> vendorNames = transactions.stream()
            .map(transaction -> transaction.getVendor().getName())
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
        if (vendorNames.isEmpty()) {
            return "";
        }
        if (vendorNames.size() == 1) {
            return vendorNames.getFirst();
        }
        return vendorNames.getFirst() + " 외 " + (vendorNames.size() - 1) + "곳";
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "";
    }

    private String formatMoney(Long amount) {
        return MONEY_FORMATTER.format(Objects.requireNonNullElse(amount, 0L)) + "원";
    }
}
