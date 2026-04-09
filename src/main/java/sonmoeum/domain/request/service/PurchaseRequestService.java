package sonmoeum.domain.request.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.request.entity.PurchaseRequest;
import sonmoeum.domain.request.enums.RequestStatus;
import sonmoeum.domain.request.exception.RequestAlreadyProcessedException;
import sonmoeum.domain.request.exception.RequestForbiddenException;
import sonmoeum.domain.request.exception.RequestNotFoundException;
import sonmoeum.domain.request.repository.PurchaseRequestRepository;
import sonmoeum.domain.request.v1.dto.request.CreatePurchaseRequestRequest;
import sonmoeum.domain.request.v1.dto.response.PurchaseRequestResponse;
import sonmoeum.domain.subject.entity.Subject;
import sonmoeum.domain.subject.service.SubjectProxyService;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.service.UserProxyService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final SubjectProxyService subjectProxyService;
    private final UserProxyService userProxyService;

    @Transactional
    public PurchaseRequestResponse createPurchaseRequest(Long requesterId, CreatePurchaseRequestRequest request) {
        log.debug("구입 요청 생성 (requesterId={}, subjectId={})", requesterId, request.subjectId());

        Subject subject = subjectProxyService.getById(request.subjectId());
        User requester = userProxyService.getById(requesterId);

        PurchaseRequest purchaseRequest = new PurchaseRequest(
            subject, requester, request.title(), request.content(), request.price()
        );
        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);

        log.debug("구입 요청 생성 완료 (id={})", saved.getId());
        return PurchaseRequestResponse.from(saved);
    }

    public List<PurchaseRequestResponse> getPurchaseRequests(
        Long requesterId, boolean isAdmin, RequestStatus status
    ) {
        log.debug("구입 요청 목록 조회 (isAdmin={}, status={})", isAdmin, status);

        List<PurchaseRequest> list;
        if (status != null) {
            list = isAdmin
                ? purchaseRequestRepository.findAllByStatusOrderByCreatedAtDesc(status)
                : purchaseRequestRepository.findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
                    status, requesterId
                );
        } else {
            list = isAdmin
                ? purchaseRequestRepository.findAllByOrderByCreatedAtDesc()
                : purchaseRequestRepository.findAllByRequestedBy_IdOrderByCreatedAtDesc(requesterId);
        }

        return list.stream().map(PurchaseRequestResponse::from).toList();
    }

    public PurchaseRequestResponse getPurchaseRequest(Long requesterId, Long requestId, boolean isAdmin) {
        log.debug("구입 요청 상세 조회 (requestId={})", requestId);
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!isAdmin && !purchaseRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new RequestForbiddenException();
        }

        return PurchaseRequestResponse.from(purchaseRequest);
    }

    @Transactional
    public PurchaseRequestResponse approvePurchaseRequest(Long approverId, Long requestId) {
        log.debug("구입 요청 승인 (requestId={})", requestId);
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (purchaseRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }

        User approver = userProxyService.getById(approverId);

        purchaseRequest.approve(approver);

        log.debug("구입 요청 승인 완료 (requestId={})", requestId);
        return PurchaseRequestResponse.from(purchaseRequest);
    }

    @Transactional
    public PurchaseRequestResponse rejectPurchaseRequest(Long approverId, Long requestId, String note) {
        log.debug("구입 요청 반려 (requestId={})", requestId);
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (purchaseRequest.getStatus() != RequestStatus.PENDING) {
            throw new RequestAlreadyProcessedException();
        }
        User approver = userProxyService.getById(approverId);

        purchaseRequest.reject(approver, note);

        log.debug("구입 요청 반려 완료 (requestId={})", requestId);
        return PurchaseRequestResponse.from(purchaseRequest);
    }
}
