package geumjeongyahak.domain.purchase_request.service;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.exception.PurchaseRequestErrorCode;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseRequestReconfirmationService {

    private final PurchaseRequestRepository purchaseRequestRepository;

    public void requestReconfirmation(Long requesterId, Long requestId) {
        log.debug("구입 요청 재확인 요청 (requesterId={}, requestId={})", requesterId, requestId);
        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException(PurchaseRequestErrorCode.NOT_FOUND, requestId));

        if (!purchaseRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new BusinessException(PurchaseRequestErrorCode.FORBIDDEN);
        }
        if (purchaseRequest.getStatus() != PurchaseRequestStatus.PURCHASED) {
            throw new BusinessException(PurchaseRequestErrorCode.INVALID_STATUS);
        }

        // TODO: NotificationService 추가 후 결재 확인 담당자에게 재확인 알림을 발송한다.
        log.info("구입 요청 재확인 알림 예약 TODO (requestId={}, requesterId={})", requestId, requesterId);
    }
}
