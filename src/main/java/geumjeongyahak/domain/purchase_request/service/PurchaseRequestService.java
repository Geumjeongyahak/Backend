package geumjeongyahak.domain.purchase_request.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.service.DepartmentProxyService;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.service.FileProxyService;
import geumjeongyahak.domain.notification.enums.PushRequestType;
import geumjeongyahak.domain.notification.event.PurchaseStatusChangedPushEvent;
import geumjeongyahak.domain.notification.event.RequestReviewedPushEvent;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestItem;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestPaymentTransaction;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentType;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.exception.PurchaseRequestErrorCode;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestSpecs;
import geumjeongyahak.domain.purchase_request.v1.dto.request.CreatePurchaseRequestByAdminRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.CreatePurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.PurchaseRequestListRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.ReportPurchaseRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.UpdatePurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestDetailResponse;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestSummaryResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import geumjeongyahak.domain.vendor.entity.Vendor;
import geumjeongyahak.domain.vendor.service.VendorService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final FileProxyService fileProxyService;
    private final ClassroomProxyService classroomProxyService;
    private final DepartmentProxyService departmentProxyService;
    private final UserProxyService userProxyService;
    private final VendorService vendorService;
    private final EventPublisher eventPublisher;
    private final PurchaseRequestProposalService purchaseRequestProposalService;

    @Transactional
    public PurchaseRequestDetailResponse createPurchaseRequest(
        Long requesterId, CreatePurchaseRequestRequest request
    ) {
        log.debug(
            "구입 요청 생성 (requesterId={}, classroomId={}, departmentId={})",
            requesterId,
            request.classroomId(),
            request.departmentId()
        );

        Classroom classroom = classroomProxyService.getActiveById(request.classroomId());
        Department department = departmentProxyService.getById(request.departmentId());
        User requester = userProxyService.getById(requesterId);
        List<PurchaseRequestItem> items = request.items().stream()
            .map(item -> new PurchaseRequestItem(
                item.name(),
                item.reason(),
                item.quantity()
            ))
            .toList();

        return createPurchaseRequest(
            requester,
            classroom,
            department,
            request.paymentType(),
            request.title(),
            request.content(),
            items
        );
    }

    @Transactional
    public PurchaseRequestDetailResponse createPurchaseRequestByAdmin(
        Long actorId, CreatePurchaseRequestByAdminRequest request
    ) {
        log.debug(
            "관리자 구입 요청 대리 생성 (actorId={}, requestedById={}, classroomId={}, departmentId={})",
            actorId,
            request.requestedById(),
            request.classroomId(),
            request.departmentId()
        );

        Classroom classroom = classroomProxyService.getActiveById(request.classroomId());
        Department department = departmentProxyService.getById(request.departmentId());
        User requester = userProxyService.getById(request.requestedById());
        List<PurchaseRequestItem> items = request.items().stream()
            .map(item -> new PurchaseRequestItem(
                item.name(),
                item.reason(),
                item.quantity()
            ))
            .toList();

        return createPurchaseRequest(
            requester,
            classroom,
            department,
            request.paymentType(),
            request.title(),
            request.content(),
            items
        );
    }

    public PaginationResponse<PurchaseRequestSummaryResponse> getPurchaseRequests(
        Long requesterId,
        PurchaseRequestListRequest request,
        boolean mine
    ) {
        log.debug(
            "구입 요청 목록 조회 (requesterId={}, mine={}, status={}, paymentType={}, keyword={}, classroomName={}, requestedByName={}, page={}, size={}, sort={})",
            requesterId,
            mine,
            request.getStatus(),
            request.getPaymentType(),
            request.getKeyword(),
            request.getClassroomName(),
            request.getRequestedByName(),
            request.getPage(),
            request.getSize(),
            request.getSort()
        );

        Specification<PurchaseRequest> spec = buildListSpecification(
            requesterId,
            request,
            mine
        );
        Page<PurchaseRequest> page = purchaseRequestRepository.findAll(spec, request.toRequest());

        return PaginationResponse.from(page, PurchaseRequestSummaryResponse::from);
    }

    private Specification<PurchaseRequest> buildListSpecification(
        Long requesterId,
        PurchaseRequestListRequest request,
        boolean mine
    ) {
        Specification<PurchaseRequest> spec = Specification.allOf(
            PurchaseRequestSpecs.isNotDeleted(),
            PurchaseRequestSpecs.hasStatus(request.getStatus()),
            PurchaseRequestSpecs.hasPaymentType(request.getPaymentType()),
            PurchaseRequestSpecs.keywordContains(request.getKeyword()),
            PurchaseRequestSpecs.classroomNameContains(request.getClassroomName()),
            PurchaseRequestSpecs.requestedByNameContains(request.getRequestedByName())
        );
        return mine
            ? spec.and(PurchaseRequestSpecs.requestedBy(requesterId))
            : spec;
    }

    public PurchaseRequestDetailResponse getPurchaseRequest(Long requesterId, Long requestId, boolean isAdmin) {
        PurchaseRequest purchaseRequest = findById(requestId);
        checkAccess(purchaseRequest, requesterId, isAdmin);
        return toDetailResponse(purchaseRequest);
    }

    public PurchaseRequestDetailResponse getPurchaseRequest(Long requestId) {
        return toDetailResponse(findById(requestId));
    }

    @Transactional
    public PurchaseRequestDetailResponse updatePurchaseRequest(
        Long requesterId, Long requestId, UpdatePurchaseRequestRequest request, boolean isAdmin
    ) {
        log.debug("구입 요청 수정 (requesterId={}, requestId={})", requesterId, requestId);
        PurchaseRequest purchaseRequest = findById(requestId);
        checkAccess(purchaseRequest, requesterId, isAdmin);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.PENDING) {
            throw new BusinessException(PurchaseRequestErrorCode.INVALID_STATUS);
        }

        Classroom classroom = classroomProxyService.getActiveById(request.classroomId());
        Department department = departmentProxyService.getById(request.departmentId());
        List<PurchaseRequestItem> items = request.items().stream()
            .map(item -> new PurchaseRequestItem(
                item.name(),
                item.reason(),
                item.quantity()
            ))
            .toList();

        purchaseRequest.update(classroom, department, request.title(), request.content(), items);

        log.debug("구입 요청 수정 완료 (id={})", purchaseRequest.getId());
        return toDetailResponse(purchaseRequest);
    }

    @Transactional
    public PurchaseRequestDetailResponse approvePurchaseRequest(Long approverId, Long requestId, String note) {
        log.debug("구입 요청 승인 (requestId={})", requestId);
        PurchaseRequest purchaseRequest = findById(requestId);
        String processedNote = requireNote(note);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.PENDING) {
            throw new BusinessException(PurchaseRequestErrorCode.ALREADY_PROCESSED);
        }

        User approver = userProxyService.getById(approverId);
        purchaseRequest.approve(approver, processedNote);
        eventPublisher.publish(RequestReviewedPushEvent.approved(
            purchaseRequest.getRequestedBy().getId(),
            purchaseRequest.getId(),
            PushRequestType.PURCHASE,
            approverId,
            "구입 요청이 승인되었습니다.",
            "구입 요청이 승인되었습니다. 구매 완료 보고를 진행해주세요.",
            processedNote
        ));

        log.debug("구입 요청 승인 완료 (requestId={})", requestId);
        return toDetailResponse(purchaseRequest);
    }

    @Transactional
    public PurchaseRequestDetailResponse rejectPurchaseRequest(Long approverId, Long requestId, String note) {
        log.debug("구입 요청 반려 (requestId={})", requestId);
        PurchaseRequest purchaseRequest = findById(requestId);
        String processedNote = requireNote(note);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.PENDING) {
            throw new BusinessException(PurchaseRequestErrorCode.ALREADY_PROCESSED);
        }

        purchaseRequest.reject(userProxyService.getById(approverId), processedNote);
        eventPublisher.publish(RequestReviewedPushEvent.rejected(
            purchaseRequest.getRequestedBy().getId(),
            purchaseRequest.getId(),
            PushRequestType.PURCHASE,
            approverId,
            "구입 요청이 반려되었습니다.",
            "구입 요청이 반려되었습니다. 반려 사유를 확인해주세요.",
            processedNote
        ));

        log.debug("구입 요청 반려 완료 (requestId={})", requestId);
        return toDetailResponse(purchaseRequest);
    }

    @Transactional
    public PurchaseRequestDetailResponse reportPurchase(
        Long requesterId, Long requestId, ReportPurchaseRequest request, boolean isAdmin
    ) {
        log.debug("구매 완료 보고 (requestId={})", requestId);
        PurchaseRequest purchaseRequest = findById(requestId);
        checkAccess(purchaseRequest, requesterId, isAdmin);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.APPROVED) {
            throw new BusinessException(PurchaseRequestErrorCode.INVALID_STATUS);
        }

        if (purchaseRequest.getApprovalAt().plusDays(7).isBefore(LocalDateTime.now())) {
            throw new BusinessException(PurchaseRequestErrorCode.PURCHASE_DEADLINE_EXCEEDED);
        }

        purchaseRequest.replaceTransactions(toTransactions(purchaseRequest, request));

        purchaseRequest.reportPurchase();

        log.debug("구매 완료 보고 완료 (requestId={}, totalPrice={})", requestId, purchaseRequest.getTotalPrice());
        return toDetailResponse(purchaseRequest);
    }

    @Transactional
    public PurchaseRequestDetailResponse updateItemReceipts(
        Long requesterId, Long requestId, ReportPurchaseRequest request, boolean isAdmin
    ) {
        PurchaseRequest purchaseRequest = findById(requestId);
        checkAccess(purchaseRequest, requesterId, isAdmin);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.PURCHASED) {
            throw new BusinessException(PurchaseRequestErrorCode.INVALID_STATUS);
        }

        purchaseRequest.replaceTransactions(toTransactions(purchaseRequest, request));

        return toDetailResponse(purchaseRequest);
    }

    @Transactional
    public PurchaseRequestDetailResponse confirmPurchase(Long confirmerId, Long requestId) {
        log.debug("구매 결재 확인 (requestId={})", requestId);
        PurchaseRequest purchaseRequest = findById(requestId);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.PURCHASED) {
            throw new BusinessException(PurchaseRequestErrorCode.INVALID_STATUS);
        }

        validateConfirmable(purchaseRequest);
        User confirmer = userProxyService.getById(confirmerId);
        if (purchaseRequest.getPaymentType() == PurchasePaymentType.PREPAID) {
            PurchaseRequestPaymentTransaction transaction = purchaseRequest.getTransactions().getFirst();
            vendorService.chargeForPurchaseRequest(
                transaction.getVendor(),
                purchaseRequest,
                purchaseRequest.getTotalPrice(),
                transaction.getReceiptFile(),
                confirmer
            );
        } else {
            Map<Vendor, Long> amountByVendor = purchaseRequest.getTransactions().stream()
                .collect(Collectors.groupingBy(
                    PurchaseRequestPaymentTransaction::getVendor,
                    Collectors.summingLong(PurchaseRequestPaymentTransaction::getAmount)
                ));
            amountByVendor.forEach((vendor, amount) ->
                vendorService.deductForPurchaseRequest(vendor, purchaseRequest, amount, confirmer)
            );
        }

        purchaseRequest.confirm();

        eventPublisher.publish(new PurchaseStatusChangedPushEvent(
            purchaseRequest.getRequestedBy().getId(),
            purchaseRequest.getId(),
            "CONFIRMED",
            "구입 요청 결재 확인 완료",
            "구입 요청에 대한 최종 결재가 확인되었습니다."
        ));

        log.debug("구매 결재 확인 완료 (requestId={})", requestId);
        return toDetailResponse(purchaseRequest);
    }

    @Transactional
    public void deletePurchaseRequest(Long requesterId, Long requestId, boolean isAdmin) {
        log.debug("구입 요청 삭제 (requesterId={}, requestId={})", requesterId, requestId);
        PurchaseRequest purchaseRequest = findById(requestId);
        checkAccess(purchaseRequest, requesterId, isAdmin);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.PENDING) {
            throw new BusinessException(PurchaseRequestErrorCode.ALREADY_PROCESSED);
        }

        purchaseRequest.softDelete();
        log.debug("구입 요청 삭제 완료 (requestId={})", requestId);
    }

    private PurchaseRequest findById(Long requestId) {
        return purchaseRequestRepository.findByIdAndIsDeletedFalse(requestId)
            .orElseThrow(() -> new ResourceNotFoundException(PurchaseRequestErrorCode.NOT_FOUND, requestId));
    }

    private List<PurchaseRequestPaymentTransaction> toTransactions(
        PurchaseRequest purchaseRequest,
        ReportPurchaseRequest request
    ) {
        if (request == null || request.transactions() == null || request.transactions().isEmpty()) {
            throw new BusinessException(PurchaseRequestErrorCode.INVALID_TRANSACTION_STRUCTURE);
        }
        request.transactions().forEach(this::validateTransactionReport);

        List<PurchaseRequestPaymentTransaction> transactions = request.transactions().stream()
            .map(transaction -> new PurchaseRequestPaymentTransaction(
                vendorService.getActiveById(transaction.vendorId()),
                normalizeItemNames(transaction.itemNames()),
                transaction.amount(),
                purchaseRequest.getPaymentType() == PurchasePaymentType.PREPAID
                    ? transaction.paymentMethod()
                    : null,
                getActiveFileOrNull(transaction.receiptFileId())
            ))
            .toList();
        validateTransactionStructure(purchaseRequest, transactions);
        return transactions;
    }

    private void validateTransactionReport(ReportPurchaseRequest.TransactionReport transaction) {
        if (transaction == null
            || transaction.vendorId() == null
            || transaction.amount() == null
            || transaction.amount() <= 0
            || transaction.itemNames() == null
            || transaction.itemNames().isEmpty()
            || transaction.itemNames().stream().anyMatch(itemName -> itemName == null || itemName.isBlank())) {
            throw new BusinessException(PurchaseRequestErrorCode.INVALID_TRANSACTION_STRUCTURE);
        }
    }

    private List<String> normalizeItemNames(List<String> itemNames) {
        List<String> normalized = itemNames.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();
        if (normalized.isEmpty()) {
            throw new BadRequestException(CommonErrorCode.MISSING_REQUIRED_FIELD, "거래 품목명은 필수입니다.");
        }
        return normalized;
    }

    private File getActiveFileOrNull(UUID fileId) {
        return fileId != null ? fileProxyService.getActiveById(fileId) : null;
    }

    private void validateConfirmable(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getTransactions().isEmpty()) {
            throw new BusinessException(PurchaseRequestErrorCode.INVALID_STATUS);
        }
        validateTransactionStructure(purchaseRequest, purchaseRequest.getTransactions());
        for (PurchaseRequestPaymentTransaction transaction : purchaseRequest.getTransactions()) {
            if (transaction.getVendor() == null
                || transaction.getAmount() == null
                || transaction.getAmount() <= 0
                || transaction.getItemNames().isEmpty()) {
                throw new BusinessException(PurchaseRequestErrorCode.INVALID_STATUS);
            }
        }
        if (purchaseRequest.getPaymentType() == PurchasePaymentType.PREPAID) {
            purchaseRequestProposalService.validateForConfirmation(purchaseRequest);
            boolean hasActiveTransactionReceipt = purchaseRequest.getTransactions().stream()
                .map(PurchaseRequestPaymentTransaction::getReceiptFile)
                .anyMatch(receiptFile -> receiptFile != null && !receiptFile.isDeleted());
            if (!hasActiveTransactionReceipt
                && !purchaseRequestProposalService.hasActiveReceipt(purchaseRequest)) {
                throw new BusinessException(PurchaseRequestErrorCode.PREPAID_RECEIPT_REQUIRED);
            }
        }
    }

    private void validateTransactionStructure(
        PurchaseRequest purchaseRequest,
        List<PurchaseRequestPaymentTransaction> transactions
    ) {
        Map<String, Long> expectedItemCounts = purchaseRequest.getItems().stream()
            .map(PurchaseRequestItem::getName)
            .map(String::trim)
            .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
        Map<String, Long> reportedItemCounts = transactions.stream()
            .flatMap(transaction -> transaction.getItemNames().stream())
            .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

        if (!expectedItemCounts.equals(reportedItemCounts)) {
            throw new BusinessException(PurchaseRequestErrorCode.INVALID_TRANSACTION_STRUCTURE);
        }

        if (purchaseRequest.getPaymentType() == PurchasePaymentType.PREPAID) {
            if (transactions.size() != 1 || transactions.getFirst().getPaymentMethod() == null) {
                throw new BusinessException(PurchaseRequestErrorCode.INVALID_TRANSACTION_STRUCTURE);
            }
            return;
        }

        boolean everyTransactionHasOneItem = transactions.stream()
            .allMatch(transaction -> transaction.getItemNames().size() == 1);
        if (transactions.size() != purchaseRequest.getItems().size() || !everyTransactionHasOneItem) {
            throw new BusinessException(PurchaseRequestErrorCode.INVALID_TRANSACTION_STRUCTURE);
        }
    }

    private PurchaseRequestDetailResponse toDetailResponse(PurchaseRequest purchaseRequest) {
        return PurchaseRequestDetailResponse.from(
            purchaseRequest,
            vendorService.getVendors(null),
            purchaseRequestProposalService.toResponse(purchaseRequest)
        );
    }

    private PurchaseRequestDetailResponse createPurchaseRequest(
        User requester,
        Classroom classroom,
        Department department,
        PurchasePaymentType paymentType,
        String title,
        String content,
        List<PurchaseRequestItem> items
    ) {
        PurchaseRequest saved = purchaseRequestRepository.save(
            new PurchaseRequest(
                classroom,
                department,
                requester,
                paymentType,
                title,
                content,
                items
            )
        );

        log.debug("구입 요청 생성 완료 (id={})", saved.getId());
        return toDetailResponse(saved);
    }

    private void checkAccess(PurchaseRequest purchaseRequest, Long requesterId, boolean isAdmin) {
        if (!isAdmin && !purchaseRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new BusinessException(PurchaseRequestErrorCode.FORBIDDEN);
        }
    }

    private String requireNote(String note) {
        if (!StringUtils.hasText(note)) {
            throw new BadRequestException(CommonErrorCode.MISSING_REQUIRED_FIELD, "처리 사유는 필수입니다.");
        }
        return note.trim();
    }
}
