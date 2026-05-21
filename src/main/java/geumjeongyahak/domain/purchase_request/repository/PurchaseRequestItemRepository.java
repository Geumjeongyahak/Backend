package geumjeongyahak.domain.purchase_request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestItem;

public interface PurchaseRequestItemRepository extends JpaRepository<PurchaseRequestItem, Long> {
}
