package geumjeongyahak.domain.request.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import geumjeongyahak.domain.request.entity.PurchaseRequest;
import geumjeongyahak.domain.request.enums.RequestStatus;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    List<PurchaseRequest> findAllByOrderByCreatedAtDesc();

    List<PurchaseRequest> findAllByRequestedBy_IdOrderByCreatedAtDesc(Long requestedById);

    List<PurchaseRequest> findAllByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<PurchaseRequest> findAllByStatusAndRequestedBy_IdOrderByCreatedAtDesc(
        RequestStatus status,
        Long requestedById
    );
}
