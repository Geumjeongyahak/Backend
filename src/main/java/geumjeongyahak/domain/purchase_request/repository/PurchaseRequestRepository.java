package geumjeongyahak.domain.purchase_request.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    long countByStatus(PurchaseRequestStatus status);

    List<PurchaseRequest> findAllByOrderByCreatedAtDesc();

    List<PurchaseRequest> findAllByRequestedBy_IdOrderByCreatedAtDesc(Long requestedById);

    List<PurchaseRequest> findAllByStatusOrderByCreatedAtDesc(PurchaseRequestStatus status);

    List<PurchaseRequest> findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
        PurchaseRequestStatus status,
        Long requestedById
    );
}
