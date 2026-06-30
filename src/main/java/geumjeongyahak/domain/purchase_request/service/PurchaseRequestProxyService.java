package geumjeongyahak.domain.purchase_request.service;

import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseRequestProxyService {

    private final PurchaseRequestRepository purchaseRequestRepository;

    public boolean existsActiveByRequesterId(Long requesterId) {
        return purchaseRequestRepository.existsByRequestedBy_IdAndStatusIn(
            requesterId,
            List.of(
                PurchaseRequestStatus.PENDING,
                PurchaseRequestStatus.APPROVED
            )
        );
    }
}
