package geumjeongyahak.domain.purchase_request.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestPaymentTransaction;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposal;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalApprovalLine;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalBudget;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalItem;
import geumjeongyahak.domain.purchase_request.enums.PurchaseBudgetItemCategory;
import geumjeongyahak.domain.purchase_request.enums.PurchaseCalculationDetail;
import geumjeongyahak.domain.purchase_request.enums.PurchaseDocumentApprovalType;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentMethod;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentType;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.exception.PurchaseRequestErrorCode;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseDocumentService {

    public static final String DOCX_CONTENT_TYPE =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String TEMPLATE_PATH = "templates/docx/expense-document-template.docx";
    private static final String CHECKED = "■";
    private static final String UNCHECKED = "□";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy. MM. dd.");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Pattern DATE_TEXT_PATTERN =
        Pattern.compile("^(\\d{4})[.\\-/]\\s*(\\d{1,2})[.\\-/]\\s*(\\d{1,2})\\.?$");
    private static final Pattern INVALID_FILENAME_CHARACTER_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final int MAX_FILENAME_TITLE_LENGTH = 60;
    private static final NumberFormat MONEY_FORMATTER = NumberFormat.getNumberInstance(Locale.KOREA);
    private static final int MAX_APPROVAL_LINE_COUNT = 3;
    private static final int TEMPLATE_APPROVAL_PLACEHOLDER_COUNT = MAX_APPROVAL_LINE_COUNT * 2;
    private static final int TEMPLATE_PLACEHOLDER_START_INDEX = 1;
    private static final int RECEIPT_IMAGE_MAX_WIDTH_POINT = 451;
    private static final int RECEIPT_IMAGE_MAX_HEIGHT_POINT = 650;
    private static final Configure RENDER_CONFIG = Configure.builder()
        .bind("itemRows", new LoopRowTableRenderPolicy(true))
        .build();

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestProposalService purchaseRequestProposalService;
    private final StorageService storageService;
    private final DriveStorageService driveStorageService;

    @Transactional(readOnly = true)
    public ExpenseDocumentResult generateProposal(
        Long actorId,
        Long purchaseRequestId,
        boolean isAdmin
    ) {
        log.debug("품의서 생성 요청 (purchaseRequestId={})", purchaseRequestId);
        PurchaseRequest purchaseRequest = findPurchaseRequest(purchaseRequestId);
        checkAccess(purchaseRequest, actorId, isAdmin);
        validatePrepaidPurchaseRequest(purchaseRequest);
        if (purchaseRequest.getStatus() == PurchaseRequestStatus.REJECTED) {
            throw new BusinessException(PurchaseRequestErrorCode.PROPOSAL_DOCUMENT_UNSUPPORTED_STATUS);
        }

        return new ExpenseDocumentResult(
            renderTemplate(
                purchaseRequest,
                buildDocumentRenderData(purchaseRequest),
                DocumentSection.PROPOSAL
            ),
            buildDownloadFilename(documentTitle(purchaseRequest, DocumentSection.PROPOSAL), "품의서")
        );
    }

    @Transactional(readOnly = true)
    public ExpenseDocumentResult generateResolution(
        Long actorId,
        Long purchaseRequestId,
        boolean isAdmin
    ) {
        log.debug("결의서 생성 요청 (purchaseRequestId={})", purchaseRequestId);
        PurchaseRequest purchaseRequest = findPurchaseRequest(purchaseRequestId);
        checkAccess(purchaseRequest, actorId, isAdmin);
        validatePrepaidPurchaseRequest(purchaseRequest);
        if (purchaseRequest.getStatus() != PurchaseRequestStatus.CONFIRMED) {
            throw new BusinessException(PurchaseRequestErrorCode.RESOLUTION_DOCUMENT_UNSUPPORTED_STATUS);
        }
        if (purchaseRequest.getProposal() == null
            || purchaseRequest.getProposal().getCompletionDate() == null) {
            throw new BusinessException(PurchaseRequestErrorCode.RESOLUTION_DOCUMENT_COMPLETION_DATE_REQUIRED);
        }

        return new ExpenseDocumentResult(
            renderTemplate(
                purchaseRequest,
                buildDocumentRenderData(purchaseRequest),
                DocumentSection.RESOLUTION
            ),
            buildDownloadFilename(documentTitle(purchaseRequest, DocumentSection.RESOLUTION), "결의서")
        );
    }

    public record ExpenseDocumentResult(byte[] content, String filename) {
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

    private byte[] renderTemplate(
        PurchaseRequest purchaseRequest,
        Map<String, Object> data,
        DocumentSection section
    ) {
        try (
            ByteArrayInputStream templateInputStream = new ByteArrayInputStream(loadTemplate());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            XWPFTemplate template = XWPFTemplate.compile(templateInputStream, RENDER_CONFIG).render(data)
        ) {
            XWPFDocument document = template.getXWPFDocument();
            retainDocumentSection(document, section);
            if (section == DocumentSection.RESOLUTION) {
                appendReceiptImages(document, activeReceiptFiles(purchaseRequest));
            }
            template.write(outputStream);
            return outputStream.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            log.error("지출증빙서류 DOCX 렌더링 실패", e);
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_GENERATION_FAILED);
        }
    }

    private void checkAccess(PurchaseRequest purchaseRequest, Long actorId, boolean isAdmin) {
        if (!isAdmin && !purchaseRequest.getRequestedBy().getId().equals(actorId)) {
            throw new BusinessException(PurchaseRequestErrorCode.FORBIDDEN);
        }
    }

    private PurchaseRequest findPurchaseRequest(Long purchaseRequestId) {
        return purchaseRequestRepository.findByIdAndIsDeletedFalse(purchaseRequestId)
            .orElseThrow(() -> new ResourceNotFoundException(PurchaseRequestErrorCode.NOT_FOUND, purchaseRequestId));
    }

    private void validatePrepaidPurchaseRequest(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getPaymentType() != PurchasePaymentType.PREPAID) {
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_ONLY_PREPAID_ALLOWED);
        }
    }

    private Map<String, Object> buildDocumentRenderData(PurchaseRequest purchaseRequest) {
        Map<String, Object> data = new HashMap<>();
        fillAllKnownPlaceholders(data);
        data.put("requesterName", purchaseRequest.getRequestedBy().getName());
        PurchaseRequestProposal proposal = purchaseRequest.getProposal();

        if (proposal == null) {
            data.put("draftTitle", purchaseRequest.getTitle());
            clearProposalRenderData(data);
            return data;
        }

        List<ApprovalLine> draftApprovals = storedApprovalLines(
            proposal,
            PurchaseDocumentApprovalType.DRAFT_APPROVAL
        );
        List<ApprovalLine> draftCooperations = storedApprovalLines(
            proposal,
            PurchaseDocumentApprovalType.DRAFT_COOPERATION
        );
        List<ApprovalLine> resolutionApprovals = storedApprovalLines(
            proposal,
            PurchaseDocumentApprovalType.RESOLUTION_APPROVAL
        );
        data.put("draftTitle", defaultText(proposal.getProposalTitle(), purchaseRequest.getTitle()));
        data.put("completionDate", formatKoreanDate(formatDate(proposal.getCompletionDate())));
        fillApprovalLines(data, "draftApproval", draftApprovals);
        fillApprovalLines(data, "resolutionApproval", resolutionApprovals);

        String proposalAmount = formatNullableMoney(proposal.getProposalAmount());
        data.put("fiscalYear", proposal.getProposalDate() != null
            ? proposal.getProposalDate().getYear() + "년"
            : "");
        String proposalNumber = defaultText(purchaseRequestProposalService.calculateProposalNumber(proposal), "");
        String proposalDate = formatKoreanDate(formatDate(proposal.getProposalDate()));
        String completionDate = formatKoreanDate(formatDate(proposal.getCompletionDate()));
        PurchasePaymentMethod paymentMethod = storedPaymentMethod(purchaseRequest);
        String paymentClassification = paymentMethodLabel(paymentMethod);

        data.put("draftDocumentNumber", proposalNumber);
        data.put("resolutionDocumentNumber", toResolutionDocumentNumber(proposalNumber));
        data.put("draftOverview", defaultText(proposal.getOverview(), ""));
        data.put("policyProject", defaultText(proposal.getPolicyProject(), ""));
        data.put("unitProject", defaultText(proposal.getUnitProject(), ""));
        data.put("detailProject", defaultText(proposal.getDetailProject(), ""));
        data.put("requestDepartment", proposal.getRequestDepartment() != null
            ? proposal.getRequestDepartment().getName()
            : "");
        data.put("draftDate", proposalDate);
        data.put("draftAmount", proposalAmount);
        fillStoredBudget(data, proposal.getBudget());
        fillStoredProposalItems(data, proposal.getItems(), proposalAmount);
        data.put("initiationDate", proposalDate);
        data.put("resolutionDate", completionDate);
        data.put("resolutionTitle", defaultText(proposal.getResolutionTitle(), purchaseRequest.getTitle()));
        data.put("paymentClassification", paymentClassification);
        data.put("vendorName", summarizeVendors(purchaseRequest.getTransactions()));
        fillPaymentMethod(data, paymentMethod);
        fillApprovalLines(
            data,
            "draftCooperation",
            draftCooperations
        );
        return data;
    }

    private List<ApprovalLine> storedApprovalLines(
        PurchaseRequestProposal proposal,
        PurchaseDocumentApprovalType type
    ) {
        return proposal.getApprovalLines().stream()
            .filter(line -> line.getLineType() == type)
            .map(this::toApprovalLine)
            .toList();
    }

    private ApprovalLine toApprovalLine(PurchaseRequestProposalApprovalLine line) {
        return new ApprovalLine(line.getPosition(), line.getName());
    }

    private String toResolutionDocumentNumber(String proposalNumber) {
        return StringUtils.hasText(proposalNumber) ? proposalNumber.replaceFirst("품", "결") : "";
    }

    private PurchasePaymentMethod storedPaymentMethod(PurchaseRequest purchaseRequest) {
        return purchaseRequest.getTransactions().isEmpty()
            ? null
            : purchaseRequest.getTransactions().getFirst().getPaymentMethod();
    }

    private void clearProposalRenderData(Map<String, Object> data) {
        List.of(
            "fiscalYear",
            "draftDocumentNumber",
            "draftOverview",
            "policyProject",
            "requestDepartment",
            "unitProject",
            "draftDate",
            "detailProject",
            "draftAmount",
            "budgetItem",
            "budgetDetail",
            "itemTotalQuantity",
            "itemTotalAmount"
        ).forEach(key -> data.put(key, ""));
        data.put("itemRows", List.of());
    }

    private void fillStoredBudget(Map<String, Object> data, PurchaseRequestProposalBudget budget) {
        if (budget == null) {
            data.put("budgetItem", "");
            data.put("budgetDetail", "");
            return;
        }
        data.put("budgetItem", budget.getItemCategory() != null
            ? defaultText(
                budget.getItemCategory() == PurchaseBudgetItemCategory.DIRECT_INPUT
                    ? budget.getCustomItemCategory()
                    : budget.getItemCategory().getDisplayName(),
                ""
            )
            : "");
        data.put("budgetDetail", budget.getCalculationDetail() != null
            ? defaultText(
                budget.getCalculationDetail() == PurchaseCalculationDetail.DIRECT_INPUT
                    ? budget.getCustomCalculationDetail()
                    : budget.getCalculationDetail().getDisplayName(),
                ""
            )
            : "");
    }

    private void fillStoredProposalItems(
        Map<String, Object> data,
        List<PurchaseRequestProposalItem> items,
        String proposalAmount
    ) {
        List<Map<String, Object>> itemRows = new ArrayList<>();
        long totalQuantity = 0L;
        for (int index = 0; index < items.size(); index++) {
            PurchaseRequestProposalItem item = items.get(index);
            totalQuantity += item.getQuantity() != null ? item.getQuantity() : 0;
            itemRows.add(Map.of(
                "no", String.valueOf(index + TEMPLATE_PLACEHOLDER_START_INDEX),
                "description", defaultText(item.getContent(), ""),
                "spec", defaultText(item.getSpecification(), ""),
                "quantity", item.getQuantity() != null ? String.valueOf(item.getQuantity()) : "",
                "unitPrice", formatNullableMoney(item.getEstimatedUnitPrice()),
                "amount", formatNullableMoney(item.calculateExpectedAmount())
            ));
        }
        data.put("itemRows", itemRows);
        data.put("itemTotalQuantity", items.isEmpty() ? "" : String.valueOf(totalQuantity));
        data.put("itemTotalAmount", proposalAmount);
    }

    private void fillAllKnownPlaceholders(Map<String, Object> data) {
        List.of(
            "fiscalYear",
            "draftDocumentNumber",
            "resolutionDocumentNumber",
            "draftTitle",
            "draftOverview",
            "policyProject",
            "requestDepartment",
            "unitProject",
            "draftDate",
            "detailProject",
            "draftAmount",
            "budgetItem",
            "budgetDetail",
            "itemTotalQuantity",
            "itemTotalAmount",
            "completionDate",
            "requesterName",
            "payCash",
            "payCard",
            "payTransfer",
            "payAuto",
            "payOther",
            "initiationDate",
            "resolutionDate",
            "resolutionTitle",
            "paymentClassification",
            "vendorName"
        ).forEach(key -> data.put(key, ""));

        data.put("itemRows", List.of());
        for (int i = TEMPLATE_PLACEHOLDER_START_INDEX; i <= TEMPLATE_APPROVAL_PLACEHOLDER_COUNT; i++) {
            data.put("draftApproval" + i, "");
            data.put("draftCooperation" + i, "");
            data.put("resolutionApproval" + i, "");
        }
    }

    private void fillPaymentMethod(Map<String, Object> data, PurchasePaymentMethod paymentMethod) {
        data.put("payCash", paymentMethod == PurchasePaymentMethod.CASH ? CHECKED : UNCHECKED);
        data.put("payCard", paymentMethod == PurchasePaymentMethod.CARD ? CHECKED : UNCHECKED);
        data.put("payTransfer", paymentMethod == PurchasePaymentMethod.TRANSFER ? CHECKED : UNCHECKED);
        data.put("payAuto", paymentMethod == PurchasePaymentMethod.AUTO_TRANSFER ? CHECKED : UNCHECKED);
        data.put("payOther", paymentMethod == PurchasePaymentMethod.OTHER ? CHECKED : UNCHECKED);
    }

    private String paymentMethodLabel(PurchasePaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return "";
        }
        return switch (paymentMethod) {
            case CASH -> "현금";
            case CARD -> "카드결제";
            case TRANSFER -> "계좌이체";
            case AUTO_TRANSFER -> "자동이체";
            case OTHER -> "기타납부";
        };
    }

    private void fillApprovalLines(Map<String, Object> data, String keyPrefix, List<ApprovalLine> approvalLines) {
        if (approvalLines == null || approvalLines.isEmpty()) {
            return;
        }

        int keyIndex = TEMPLATE_PLACEHOLDER_START_INDEX;
        for (ApprovalLine line : approvalLines) {
            if (keyIndex > TEMPLATE_APPROVAL_PLACEHOLDER_COUNT) {
                return;
            }
            data.put(keyPrefix + keyIndex, defaultText(line.position(), ""));
            keyIndex++;

            if (keyIndex > TEMPLATE_APPROVAL_PLACEHOLDER_COUNT) {
                return;
            }
            data.put(keyPrefix + keyIndex, defaultText(line.name(), ""));
            keyIndex++;
        }
    }

    private void appendReceiptImages(XWPFDocument document, List<File> receiptFiles) {
        for (File receiptFile : receiptFiles) {
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

    private List<File> activeReceiptFiles(PurchaseRequest purchaseRequest) {
        Map<UUID, File> filesById = new LinkedHashMap<>();
        if (purchaseRequest.getProposal() != null) {
            purchaseRequest.getProposal().getReceipts().stream()
                .filter(receipt -> !receipt.isDeleted() && !receipt.getFile().isDeleted())
                .forEach(receipt -> filesById.putIfAbsent(receipt.getFile().getId(), receipt.getFile()));
        }
        purchaseRequest.getTransactions().stream()
            .map(PurchaseRequestPaymentTransaction::getReceiptFile)
            .filter(Objects::nonNull)
            .filter(file -> !file.isDeleted())
            .forEach(file -> filesById.putIfAbsent(file.getId(), file));
        return List.copyOf(filesById.values());
    }

    private void retainDocumentSection(XWPFDocument document, DocumentSection section) {
        List<IBodyElement> bodyElements = new ArrayList<>(document.getBodyElements());
        int resolutionTitleIndex = -1;
        for (int index = 0; index < bodyElements.size(); index++) {
            if (isResolutionSectionStart(bodyElements.get(index))) {
                resolutionTitleIndex = index;
                break;
            }
        }
        if (resolutionTitleIndex < 0) {
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_GENERATION_FAILED);
        }

        if (section == DocumentSection.RESOLUTION) {
            for (int index = resolutionTitleIndex - 1; index >= 0; index--) {
                document.removeBodyElement(index);
            }
            return;
        }

        int firstRemovedIndex = resolutionTitleIndex;
        while (firstRemovedIndex > 0
            && bodyElements.get(firstRemovedIndex - 1) instanceof XWPFParagraph paragraph
            && paragraph.getText().isBlank()) {
            firstRemovedIndex--;
        }
        for (int index = bodyElements.size() - 1; index >= firstRemovedIndex; index--) {
            document.removeBodyElement(index);
        }
    }

    private boolean isResolutionSectionStart(IBodyElement bodyElement) {
        String text;
        if (bodyElement instanceof XWPFParagraph paragraph) {
            text = paragraph.getText();
        } else if (bodyElement instanceof XWPFTable table) {
            text = table.getText();
        } else {
            return false;
        }
        return text.replaceAll("\\s+", "").contains("지출결의서");
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

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private String documentTitle(PurchaseRequest purchaseRequest, DocumentSection section) {
        PurchaseRequestProposal proposal = purchaseRequest.getProposal();
        if (proposal == null) {
            return purchaseRequest.getTitle();
        }
        return section == DocumentSection.PROPOSAL
            ? defaultText(proposal.getProposalTitle(), purchaseRequest.getTitle())
            : defaultText(proposal.getResolutionTitle(), purchaseRequest.getTitle());
    }

    private String buildDownloadFilename(String title, String documentName) {
        return "%s-%s-%s.docx".formatted(
            documentName,
            sanitizeFilenameTitle(title),
            LocalDate.now().format(FILE_DATE_FORMATTER)
        );
    }

    private String sanitizeFilenameTitle(String title) {
        String sanitized = INVALID_FILENAME_CHARACTER_PATTERN.matcher(defaultText(title, "구매요청"))
            .replaceAll(" ")
            .replaceAll("\\s+", " ")
            .trim();
        if (sanitized.isBlank()) {
            return "구매요청";
        }
        if (sanitized.length() <= MAX_FILENAME_TITLE_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_FILENAME_TITLE_LENGTH).trim();
    }

    private String formatKoreanDate(String date) {
        String value = defaultText(date, "");
        Matcher matcher = DATE_TEXT_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return value;
        }

        return "%s년 %02d월 %02d일".formatted(
            matcher.group(1),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3))
        );
    }

    private String formatMoney(Long amount) {
        return MONEY_FORMATTER.format(Objects.requireNonNullElse(amount, 0L)) + "원";
    }

    private String formatNullableMoney(Long amount) {
        return amount != null ? formatMoney(amount) : "";
    }

    private record ImageSize(int widthPoint, int heightPoint) {
    }

    private record ApprovalLine(String position, String name) {
    }

    private enum DocumentSection {
        PROPOSAL,
        RESOLUTION
    }
}
