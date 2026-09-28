package geumjeongyahak.domain.purchase_request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.exception.PurchaseRequestErrorCode;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;

/**
 * 구입 요청을 바꾸려는 쪽이 쓰는 조회. 행을 잠그고 읽는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseRequestLockReader {

    private final PurchaseRequestRepository purchaseRequestRepository;

    public PurchaseRequest getForUpdate(Long requestId) {
        try {
            return purchaseRequestRepository.findForUpdateByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(PurchaseRequestErrorCode.NOT_FOUND, requestId));
        } catch (PessimisticLockingFailureException ex) {
            // 예외 메시지에는 DB 가 행 내용을 실어 보내기도 해서 종류만 남긴다.
            log.warn("구입 요청 락 획득 실패 (requestId={}, cause={})", requestId, ex.getClass().getSimpleName());
            throw new BusinessException(PurchaseRequestErrorCode.LOCK_NOT_ACQUIRED);
        }
    }
}
