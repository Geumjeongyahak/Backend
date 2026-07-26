package geumjeongyahak.domain.purchase_request.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.purchase_request.enums.PurchaseDocumentApprovalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@Table(name = "purchase_request_proposal_approval_lines")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequestProposalApprovalLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private PurchaseRequestProposal proposal;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 40)
    private PurchaseDocumentApprovalType lineType;

    private String position;

    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public PurchaseRequestProposalApprovalLine(
        @NonNull PurchaseDocumentApprovalType lineType,
        String position,
        String name,
        int sortOrder
    ) {
        this.lineType = lineType;
        this.position = position;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    void assignProposal(PurchaseRequestProposal proposal) {
        this.proposal = proposal;
    }
}
