package geumjeongyahak.domain.purchase_request.repository;

import java.util.UUID;

import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRequestReceiptRepository extends JpaRepository<PurchaseRequestReceipt, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PurchaseRequestReceipt receipt where receipt.file.id = :fileId")
    void deleteByFileId(@Param("fileId") UUID fileId);
}
