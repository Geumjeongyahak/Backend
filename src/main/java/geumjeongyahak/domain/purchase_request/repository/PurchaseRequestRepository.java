package geumjeongyahak.domain.purchase_request.repository;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long>, JpaSpecificationExecutor<PurchaseRequest> {

    long countByStatus(PurchaseRequestStatus status);

    boolean existsByRequestedBy_IdAndStatusIn(
        Long requestedById,
        Collection<PurchaseRequestStatus> statuses
    );
}
