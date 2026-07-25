package geumjeongyahak.domain.purchase_request.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.purchase_request.enums.PurchaseBudgetItemCategory;
import geumjeongyahak.domain.purchase_request.enums.PurchaseCalculationDetail;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "purchase_request_proposal_budgets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequestProposalBudget extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false, unique = true)
    private PurchaseRequestProposal proposal;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_category", length = 40)
    private PurchaseBudgetItemCategory itemCategory;

    @Column(name = "custom_item_category")
    private String customItemCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_detail", length = 60)
    private PurchaseCalculationDetail calculationDetail;

    @Column(name = "custom_calculation_detail")
    private String customCalculationDetail;

    public PurchaseRequestProposalBudget(
        PurchaseBudgetItemCategory itemCategory,
        String customItemCategory,
        PurchaseCalculationDetail calculationDetail,
        String customCalculationDetail
    ) {
        this.itemCategory = itemCategory;
        this.customItemCategory = customItemCategory;
        this.calculationDetail = calculationDetail;
        this.customCalculationDetail = customCalculationDetail;
    }

    void assignProposal(PurchaseRequestProposal proposal) {
        this.proposal = proposal;
    }
}
