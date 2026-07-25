package geumjeongyahak.domain.purchase_request.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@Table(name = "purchase_request_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequestProposal extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id", nullable = false, unique = true)
    private PurchaseRequest purchaseRequest;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(name = "policy_project")
    private String policyProject;

    @Column(name = "unit_project")
    private String unitProject;

    @Column(name = "detail_project")
    private String detailProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_department_id")
    private Department requestDepartment;

    @Column(name = "proposal_date")
    private LocalDate proposalDate;

    @Column(name = "proposal_amount")
    private Long proposalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_account", length = 40)
    private PurchasePaymentAccount paymentAccount;

    @OneToOne(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private PurchaseRequestProposalBudget budget;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<PurchaseRequestProposalItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<PurchaseRequestProposalReceipt> receipts = new ArrayList<>();

    public PurchaseRequestProposal(@NonNull PurchaseRequest purchaseRequest) {
        this.purchaseRequest = purchaseRequest;
        purchaseRequest.assignProposal(this);
    }

    public void updateDetails(
        String overview,
        String policyProject,
        String unitProject,
        String detailProject,
        Department requestDepartment,
        LocalDate proposalDate,
        Long proposalAmount,
        PurchasePaymentAccount paymentAccount
    ) {
        this.overview = overview;
        this.policyProject = policyProject;
        this.unitProject = unitProject;
        this.detailProject = detailProject;
        this.requestDepartment = requestDepartment;
        this.proposalDate = proposalDate;
        this.proposalAmount = proposalAmount;
        this.paymentAccount = paymentAccount;
    }

    public void replaceBudget(PurchaseRequestProposalBudget budget) {
        if (budget != null) {
            budget.assignProposal(this);
        }
        this.budget = budget;
    }

    public void replaceItems(List<PurchaseRequestProposalItem> items) {
        this.items.clear();
        for (int index = 0; index < items.size(); index++) {
            PurchaseRequestProposalItem item = items.get(index);
            item.assignProposal(this, index);
            this.items.add(item);
        }
    }

    public void addReceipt(PurchaseRequestProposalReceipt receipt) {
        receipt.assignProposal(this, receipts.size());
        this.receipts.add(receipt);
    }
}
