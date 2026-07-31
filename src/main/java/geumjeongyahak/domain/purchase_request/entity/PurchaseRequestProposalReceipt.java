package geumjeongyahak.domain.purchase_request.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.file.entity.File;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@Table(name = "purchase_request_proposal_receipts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequestProposalReceipt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private PurchaseRequestProposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public PurchaseRequestProposalReceipt(@NonNull File file) {
        this.file = file;
        this.isDeleted = false;
    }

    void assignProposal(PurchaseRequestProposal proposal, int sortOrder) {
        this.proposal = proposal;
        this.sortOrder = sortOrder;
    }

    public void softDelete() {
        if (isDeleted) {
            return;
        }
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
