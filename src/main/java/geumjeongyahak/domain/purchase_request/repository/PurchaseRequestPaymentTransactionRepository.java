package geumjeongyahak.domain.purchase_request.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestPaymentTransaction;

public interface PurchaseRequestPaymentTransactionRepository extends JpaRepository<PurchaseRequestPaymentTransaction, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PurchaseRequestPaymentTransaction tx set tx.receiptFile = null where tx.receiptFile.id = :fileId")
    void clearReceiptFileByFileId(@Param("fileId") UUID fileId);
}
