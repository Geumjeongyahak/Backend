package sonmoeum.domain.request.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sonmoeum.domain.request.entity.PurchaseRequest;
import sonmoeum.domain.request.enums.RequestStatus;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    List<PurchaseRequest> findAllByOrderByCreatedAtDesc();

    List<PurchaseRequest> findAllByRequestedBy_IdOrderByCreatedAtDesc(Long requestedById);

    List<PurchaseRequest> findAllByStatusOrderByCreatedAtDesc(RequestStatus status);
}
