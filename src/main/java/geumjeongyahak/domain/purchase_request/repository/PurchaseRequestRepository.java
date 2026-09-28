package geumjeongyahak.domain.purchase_request.repository;

import java.util.Collection;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long>, JpaSpecificationExecutor<PurchaseRequest> {

    Optional<PurchaseRequest> findByIdAndIsDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PurchaseRequest> findForUpdateByIdAndIsDeletedFalse(Long id);

    long countByStatusAndIsDeletedFalse(PurchaseRequestStatus status);

    boolean existsByRequestedBy_IdAndStatusInAndIsDeletedFalse(
        Long requestedById,
        Collection<PurchaseRequestStatus> statuses
    );
}
