package geumjeongyahak.domain.purchase_request.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.service.FileProxyService;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestItem;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.exception.PurchaseRequestErrorCode;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import geumjeongyahak.domain.purchase_request.v1.dto.request.CreatePurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.ReportPurchaseRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestDetailResponse;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestSummaryResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final FileProxyService fileProxyService;
    private final ClassroomProxyService classroomProxyService;
    private final UserProxyService userProxyService;

    @Transactional
    public PurchaseRequestDetailResponse createPurchaseRequest(
        Long requesterId, Long classroomId, CreatePurchaseRequestRequest request
    ) {
        log.debug("구입 요청 생성 (requesterId={}, classroomId={})", requesterId, classroomId);

        Classroom classroom = classroomProxyService.getActiveById(classroomId);
        User requester = userProxyService.getById(requesterId);

        List<PurchaseRequestItem> items = request.items().stream()
            .map(item -> {
                File receiptFile = item.receiptFileId() != null
                    ? fileProxyService.getReferenceById(item.receiptFileId())
                    : null;
                return new PurchaseRequestItem(item.name(), item.reason(), item.price(), receiptFile);
            })
            .toList();

        PurchaseRequest saved = purchaseRequestRepository.save(
            new PurchaseRequest(classroom, requester, request.title(), request.content(), items)
        );

        log.debug("구입 요청 생성 완료 (id={})", saved.getId());
        return PurchaseRequestDetailResponse.from(saved);
    }

    public List<PurchaseRequestSummaryResponse> getPurchaseRequests(Long requesterId, PurchaseRequestStatus status) {
        log.debug("구입 요청 목록 조회 (requesterId={}, status={})", requesterId, status);

        List<PurchaseRequest> list = status != null
            ? purchaseRequestRepository.findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(status, requesterId)
            : purchaseRequestRepository.findAllByRequestedBy_IdOrderByCreatedAtDesc(requesterId);

        return list.stream().map(PurchaseRequestSummaryResponse::from).toList();
    }

    public List<PurchaseRequestSummaryResponse> getAllPurchaseRequests(PurchaseRequestStatus status) {
        log.debug("구입 요청 전체 목록 조회 (status={})", status);

        List<PurchaseRequest> list = status != null
            ? purchaseRequestRepository.findAllByStatusOrderByCreatedAtDesc(status)
            : purchaseRequestRepository.findAllByOrderByCreatedAtDesc();

        return list.stream().map(PurchaseRequestSummaryResponse::from).toList();
    }

    public PurchaseRequestDetailResponse getPurchaseRequest(Long requesterId, Long requestId, boolean isAdmin) {
        PurchaseRequest purchaseRequest = findById(requestId);
        checkAccess(purchaseRequest, requesterId, isAdmin);
        return PurchaseRequestDetailResponse.from(purchaseRequest);
    }

    @Transactional
    public PurchaseRequestDetailResponse updatePurchaseRequest(
        Long requesterId, Long requestId, CreatePurchaseRequestRequest request, boolean isAdmin
    ) {
        log.debug("구입 요청 수정 (requesterId={}, requestId={})", requesterId, requestId);
        PurchaseRequest purchaseRequest = findById(requestId);
        checkAccess(purchaseRequest, requesterId, isAdmin);

        List<PurchaseRequestItem> items = request.items().stream()
            .map(item -> {
                File receiptFile = item.receiptFileId() != null
                    ? fileProxyService.getReferenceById(item.receiptFileId())
                    : null;
                return new PurchaseRequestItem(item.name(), item.reason(), item.price(), receiptFile);
            })
            .toList();

        purchaseRequest.update(request.title(), request.content(), items);

        log.debug("구입 요청 수정 완료 (id={})", purchaseRequest.getId());
        return PurchaseRequestDetailResponse.from(purchaseRequest);
    }

    @Transactional
    public PurchaseRequestDetailResponse approvePurchaseRequest(Long approverId, Long requestId) {
        log.debug("구입 요청 승인 (requestId={})", requestId);
        PurchaseRequest purchaseRequest = findById(requestId);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.PENDING) {
            throw new BusinessException(PurchaseRequestErrorCode.ALREADY_PROCESSED);
        }

        purchaseRequest.approve(userProxyService.getById(approverId));

        log.debug("구입 요청 승인 완료 (requestId={})", requestId);
        return PurchaseRequestDetailResponse.from(purchaseRequest);
    }

    @Transactional
    public PurchaseRequestDetailResponse rejectPurchaseRequest(Long approverId, Long requestId, String note) {
        log.debug("구입 요청 반려 (requestId={})", requestId);
        PurchaseRequest purchaseRequest = findById(requestId);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.PENDING) {
            throw new BusinessException(PurchaseRequestErrorCode.ALREADY_PROCESSED);
        }

        purchaseRequest.reject(userProxyService.getById(approverId), note);

        log.debug("구입 요청 반려 완료 (requestId={})", requestId);
        return PurchaseRequestDetailResponse.from(purchaseRequest);
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

        Map<Long, PurchaseRequestItem> itemMap = purchaseRequest.getItems().stream()
            .collect(Collectors.toMap(PurchaseRequestItem::getId, i -> i));

        long totalPrice = 0;
        for (ReportPurchaseRequest.ItemReport itemReport : request.items()) {
            PurchaseRequestItem item = itemMap.get(itemReport.itemId());
            if (item == null) {
                throw new ResourceNotFoundException(PurchaseRequestErrorCode.ITEM_NOT_FOUND, itemReport.itemId());
            }
            File receiptFile = itemReport.receiptFileId() != null
                ? fileProxyService.getReferenceById(itemReport.receiptFileId())
                : null;
            item.updatePurchaseDetails(itemReport.price(), receiptFile);
            totalPrice += itemReport.price();
        }

        purchaseRequest.reportPurchase(totalPrice);

        log.debug("구매 완료 보고 완료 (requestId={}, totalPrice={})", requestId, totalPrice);
        return PurchaseRequestDetailResponse.from(purchaseRequest);
    }

    @Transactional
    public PurchaseRequestDetailResponse confirmPurchase(Long confirmerId, Long requestId) {
        log.debug("구매 결재 확인 (requestId={})", requestId);
        PurchaseRequest purchaseRequest = findById(requestId);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.PURCHASED) {
            throw new BusinessException(PurchaseRequestErrorCode.INVALID_STATUS);
        }

        purchaseRequest.confirm();

        log.debug("구매 결재 확인 완료 (requestId={})", requestId);
        return PurchaseRequestDetailResponse.from(purchaseRequest);
    }

    private PurchaseRequest findById(Long requestId) {
        return purchaseRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException(PurchaseRequestErrorCode.NOT_FOUND, requestId));
    }

    private void checkAccess(PurchaseRequest purchaseRequest, Long requesterId, boolean isAdmin) {
        if (!isAdmin && !purchaseRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new BusinessException(PurchaseRequestErrorCode.FORBIDDEN);
        }
    }
}
