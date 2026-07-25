package geumjeongyahak.domain.purchase_request.repository;

import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalReceipt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRequestProposalReceiptRepository
    extends JpaRepository<PurchaseRequestProposalReceipt, Long> {

    Optional<PurchaseRequestProposalReceipt> findByIdAndProposal_PurchaseRequest_IdAndIsDeletedFalse(
        Long receiptId,
        Long requestId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PurchaseRequestProposalReceipt receipt where receipt.file.id = :fileId")
    void deleteAllByFileId(@Param("fileId") UUID fileId);
}
