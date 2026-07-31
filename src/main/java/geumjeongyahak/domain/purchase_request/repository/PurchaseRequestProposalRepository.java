package geumjeongyahak.domain.purchase_request.repository;

import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposal;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentAccount;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRequestProposalRepository extends JpaRepository<PurchaseRequestProposal, Long> {

    @Query("""
        select count(p)
        from PurchaseRequestProposal p
        where p.paymentAccount = :paymentAccount
          and p.proposalDate >= :yearStart
          and p.proposalDate < :nextYearStart
          and p.purchaseRequest.isDeleted = false
          and p.purchaseRequest.status <> :excludedStatus
          and (
              p.proposalDate < :proposalDate
              or (
                  p.proposalDate = :proposalDate
                  and p.purchaseRequest.createdAt < :requestCreatedAt
              )
              or (
                  p.proposalDate = :proposalDate
                  and p.purchaseRequest.createdAt = :requestCreatedAt
                  and p.purchaseRequest.id <= :requestId
              )
          )
        """)
    long countNumberedRequestsThrough(
        @Param("paymentAccount") PurchasePaymentAccount paymentAccount,
        @Param("yearStart") LocalDate yearStart,
        @Param("nextYearStart") LocalDate nextYearStart,
        @Param("proposalDate") LocalDate proposalDate,
        @Param("requestCreatedAt") LocalDateTime requestCreatedAt,
        @Param("requestId") Long requestId,
        @Param("excludedStatus") PurchaseRequestStatus excludedStatus
    );
}
