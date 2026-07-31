package geumjeongyahak.domain.purchase_request.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "purchase_request_proposal_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequestProposalItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private PurchaseRequestProposal proposal;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String specification;

    private Integer quantity;

    @Column(name = "estimated_unit_price")
    private Long estimatedUnitPrice;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public PurchaseRequestProposalItem(
        String content,
        String specification,
        Integer quantity,
        Long estimatedUnitPrice
    ) {
        this.content = content;
        this.specification = specification;
        this.quantity = quantity;
        this.estimatedUnitPrice = estimatedUnitPrice;
    }

    void assignProposal(PurchaseRequestProposal proposal, int sortOrder) {
        this.proposal = proposal;
        this.sortOrder = sortOrder;
    }

    public Long calculateExpectedAmount() {
        if (quantity == null || estimatedUnitPrice == null) {
            return null;
        }
        return Math.multiplyExact(quantity.longValue(), estimatedUnitPrice);
    }
}
