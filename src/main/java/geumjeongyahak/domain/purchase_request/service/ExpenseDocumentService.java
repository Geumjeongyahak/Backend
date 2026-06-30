package geumjeongyahak.domain.purchase_request.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.service.DriveStorageService;
import geumjeongyahak.domain.file.service.StorageService;
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
    private static final int TEMPLATE_APPROVAL_SLOT_COUNT = 9;
    private static final int TEMPLATE_PLACEHOLDER_START_INDEX = 1;
    private static final int RECEIPT_IMAGE_MAX_WIDTH_POINT = 451;
    private static final int RECEIPT_IMAGE_MAX_HEIGHT_POINT = 650;
    private static final Configure RENDER_CONFIG = Configure.builder()
        .bind("itemRows", new LoopRowTableRenderPolicy(true))
        .bind("transactionRows", new LoopRowTableRenderPolicy(true))
        .build();

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final StorageService storageService;
    private final DriveStorageService driveStorageService;

    public byte[] generate(Long purchaseRequestId, GenerateExpenseDocumentRequest request) {
        log.debug("지출증빙서류 생성 요청 (purchaseRequestId={})", purchaseRequestId);
        PurchaseRequest purchaseRequest = findPurchaseRequest(purchaseRequestId);
        validateGeneratable(purchaseRequest);

        return renderTemplate(purchaseRequest, buildRenderData(purchaseRequest, request));
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

    private byte[] renderTemplate(PurchaseRequest purchaseRequest, Map<String, Object> data) {
        try (
            ByteArrayInputStream templateInputStream = new ByteArrayInputStream(loadTemplate());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            XWPFTemplate template = XWPFTemplate.compile(templateInputStream, RENDER_CONFIG).render(data)
        ) {
            appendReceiptImages(template.getXWPFDocument(), purchaseRequest.getTransactions());
            template.write(outputStream);
            return outputStream.toByteArray();
        } catch (BusinessException e) {
            throw e;
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

        fillItemRows(data, purchaseRequest.getItems(), amount);

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
        fillTransactionRows(data, purchaseRequest.getTransactions(), paymentDate);
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

        data.put("itemRows", List.of());
        data.put("transactionRows", List.of());

        for (int i = TEMPLATE_PLACEHOLDER_START_INDEX; i <= TEMPLATE_APPROVAL_SLOT_COUNT; i++) {
            data.put("draftApproval" + i, "");
            data.put("draftCooperation" + i, "");
            data.put("resolutionApproval" + i, "");
        }
    }

    private void fillItemRows(Map<String, Object> data, List<PurchaseRequestItem> items, String amount) {
        List<Map<String, Object>> itemRows = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            PurchaseRequestItem item = items.get(index);
            int row = index + TEMPLATE_PLACEHOLDER_START_INDEX;
            itemRows.add(Map.of(
                "no", String.valueOf(row),
                "description", defaultText(item.getName(), ""),
                "spec", "",
                "quantity", String.valueOf(item.getQuantity()),
                "unitPrice", "",
                "amount", ""
            ));
        }
        data.put("itemRows", itemRows);
        data.put("itemTotalQuantity", String.valueOf(items.stream().mapToInt(PurchaseRequestItem::getQuantity).sum()));
        data.put("itemTotalAmount", amount);
    }

    private void fillTransactionRows(
        Map<String, Object> data,
        List<PurchaseRequestPaymentTransaction> transactions,
        String paymentDate
    ) {
        List<Map<String, Object>> transactionRows = new ArrayList<>();
        for (int index = 0; index < transactions.size(); index++) {
            PurchaseRequestPaymentTransaction transaction = transactions.get(index);
            int row = index + TEMPLATE_PLACEHOLDER_START_INDEX;
            transactionRows.add(Map.of(
                "no", String.valueOf(row),
                "date", paymentDate,
                "detail", String.join(", ", transaction.getItemNames()),
                "amount", formatMoney(transaction.getAmount()),
                "note", defaultText(transaction.getVendor().getName(), "")
            ));
        }
        data.put("transactionRows", transactionRows);
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

    private void appendReceiptImages(
        XWPFDocument document,
        List<PurchaseRequestPaymentTransaction> transactions
    ) {
        for (PurchaseRequestPaymentTransaction transaction : transactions) {
            File receiptFile = transaction.getReceiptFile();
            if (receiptFile == null || receiptFile.isDeleted()) {
                continue;
            }

            byte[] content = downloadReceiptImage(receiptFile);
            BufferedImage image = readReceiptImage(content);
            ImageSize imageSize = fitReceiptImage(image);
            int pictureType = pictureType(receiptFile);

            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setPageBreak(true);
            paragraph.setAlignment(ParagraphAlignment.CENTER);

            XWPFRun run = paragraph.createRun();
            addReceiptPicture(run, receiptFile, content, pictureType, imageSize);
        }
    }

    private void addReceiptPicture(
        XWPFRun run,
        File receiptFile,
        byte[] content,
        int pictureType,
        ImageSize imageSize
    ) {
        try {
            run.addPicture(
                new ByteArrayInputStream(content),
                pictureType,
                defaultText(receiptFile.getOriginalName(), "receipt"),
                Units.toEMU(imageSize.widthPoint()),
                Units.toEMU(imageSize.heightPoint())
            );
        } catch (IOException | org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
            log.error("지출증빙서류 영수증 이미지 삽입 실패 (fileId={})", receiptFile.getId(), e);
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_GENERATION_FAILED);
        }
    }

    private byte[] downloadReceiptImage(File receiptFile) {
        try {
            if (receiptFile.isGoogleDriveFile()) {
                return driveStorageService.download(receiptFile.getStorageKey());
            }
            return storageService.download(receiptFile.getStorageKey());
        } catch (BusinessException e) {
            log.error("지출증빙서류 영수증 파일 읽기 실패 (fileId={})", receiptFile.getId(), e);
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_RECEIPT_READ_FAILED);
        }
    }

    private BufferedImage readReceiptImage(byte[] content) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_UNSUPPORTED_RECEIPT_IMAGE);
            }
            return image;
        } catch (IOException e) {
            log.error("지출증빙서류 영수증 이미지 읽기 실패", e);
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_RECEIPT_READ_FAILED);
        }
    }

    private ImageSize fitReceiptImage(BufferedImage image) {
        double widthScale = (double) RECEIPT_IMAGE_MAX_WIDTH_POINT / image.getWidth();
        double heightScale = (double) RECEIPT_IMAGE_MAX_HEIGHT_POINT / image.getHeight();
        double scale = Math.min(widthScale, heightScale);
        return new ImageSize(
            Math.max(1, (int) Math.round(image.getWidth() * scale)),
            Math.max(1, (int) Math.round(image.getHeight() * scale))
        );
    }

    private int pictureType(File receiptFile) {
        String contentType = defaultText(receiptFile.getContentType(), "").toLowerCase(Locale.ROOT);
        String extension = defaultText(receiptFile.getExt(), "").toLowerCase(Locale.ROOT);
        if ("image/png".equals(contentType) || "png".equals(extension)) {
            return Document.PICTURE_TYPE_PNG;
        }
        if ("image/jpeg".equals(contentType) || "image/jpg".equals(contentType)
            || "jpeg".equals(extension) || "jpg".equals(extension)) {
            return Document.PICTURE_TYPE_JPEG;
        }
        if ("image/gif".equals(contentType) || "gif".equals(extension)) {
            return Document.PICTURE_TYPE_GIF;
        }
        if ("image/bmp".equals(contentType) || "bmp".equals(extension)) {
            return Document.PICTURE_TYPE_BMP;
        }
        throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_UNSUPPORTED_RECEIPT_IMAGE);
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

    private record ImageSize(int widthPoint, int heightPoint) {
    }
}
