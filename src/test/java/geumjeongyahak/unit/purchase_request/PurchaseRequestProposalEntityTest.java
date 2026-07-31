package geumjeongyahak.unit.purchase_request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestItem;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposal;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalApprovalLine;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalBudget;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalItem;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalReceipt;
import geumjeongyahak.domain.purchase_request.enums.PurchaseBudgetItemCategory;
import geumjeongyahak.domain.purchase_request.enums.PurchaseCalculationDetail;
import geumjeongyahak.domain.purchase_request.enums.PurchaseDocumentApprovalType;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentAccount;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentType;
import geumjeongyahak.domain.users.entity.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PurchaseRequestProposalEntityTest {

    @Test
    void constructor_linksProposalToPurchaseRequest() {
        PurchaseRequest purchaseRequest = createPurchaseRequest();

        PurchaseRequestProposal proposal = new PurchaseRequestProposal(purchaseRequest);

        assertThat(proposal.getPurchaseRequest()).isSameAs(purchaseRequest);
        assertThat(purchaseRequest.getProposal()).isSameAs(proposal);
        assertThat(proposal.getBudget()).isNull();
        assertThat(proposal.getItems()).isEmpty();
        assertThat(proposal.getReceipts()).isEmpty();
    }

    @Test
    void updateDetails_allowsOptionalFieldsForDraft() {
        PurchaseRequestProposal proposal = new PurchaseRequestProposal(createPurchaseRequest());
        Department department = mock(Department.class);
        LocalDate proposalDate = LocalDate.of(2026, 7, 25);

        proposal.updateDetails(
            "7월 교재 구입",
            "7월 교재비 지출",
            LocalDate.of(2026, 7, 30),
            "교재 구매 품의",
            "2026년 성인문해교육 지원사업",
            "프로그램운영비",
            "교재비",
            department,
            proposalDate,
            20_000L,
            PurchasePaymentAccount.NATIONAL_SUBSIDY_04
        );

        assertThat(proposal.getProposalTitle()).isEqualTo("7월 교재 구입");
        assertThat(proposal.getResolutionTitle()).isEqualTo("7월 교재비 지출");
        assertThat(proposal.getCompletionDate()).isEqualTo(LocalDate.of(2026, 7, 30));
        assertThat(proposal.getOverview()).isEqualTo("교재 구매 품의");
        assertThat(proposal.getPolicyProject()).isEqualTo("2026년 성인문해교육 지원사업");
        assertThat(proposal.getUnitProject()).isEqualTo("프로그램운영비");
        assertThat(proposal.getDetailProject()).isEqualTo("교재비");
        assertThat(proposal.getRequestDepartment()).isSameAs(department);
        assertThat(proposal.getProposalDate()).isEqualTo(proposalDate);
        assertThat(proposal.getProposalAmount()).isEqualTo(20_000L);
        assertThat(proposal.getPaymentAccount()).isEqualTo(PurchasePaymentAccount.NATIONAL_SUBSIDY_04);

        proposal.updateDetails(null, null, null, null, null, null, null, null, null, null, null);

        assertThat(proposal.getProposalTitle()).isNull();
        assertThat(proposal.getCompletionDate()).isNull();
        assertThat(proposal.getOverview()).isNull();
        assertThat(proposal.getProposalDate()).isNull();
        assertThat(proposal.getProposalAmount()).isNull();
        assertThat(proposal.getPaymentAccount()).isNull();
    }

    @Test
    void replaceApprovalLines_assignsStoredDocumentLines() {
        PurchaseRequestProposal proposal = new PurchaseRequestProposal(createPurchaseRequest());
        PurchaseRequestProposalApprovalLine approval = new PurchaseRequestProposalApprovalLine(
            PurchaseDocumentApprovalType.DRAFT_APPROVAL,
            "총무",
            "관리자",
            0
        );

        proposal.replaceApprovalLines(List.of(approval));

        assertThat(proposal.getApprovalLines()).containsExactly(approval);
        assertThat(approval.getProposal()).isSameAs(proposal);
        assertThat(approval.getLineType()).isEqualTo(PurchaseDocumentApprovalType.DRAFT_APPROVAL);
        assertThat(approval.getPosition()).isEqualTo("총무");
        assertThat(approval.getName()).isEqualTo("관리자");
        assertThat(approval.getSortOrder()).isZero();
    }

    @Test
    void replaceBudgetAndItems_assignsAggregateAndCalculatesExpectedAmount() {
        PurchaseRequestProposal proposal = new PurchaseRequestProposal(createPurchaseRequest());
        PurchaseRequestProposalBudget budget = new PurchaseRequestProposalBudget(
            PurchaseBudgetItemCategory.TEXTBOOK,
            null,
            PurchaseCalculationDetail.COMMERCIAL_TEXTBOOK,
            null
        );
        PurchaseRequestProposalItem firstItem = new PurchaseRequestProposalItem("국어 교재", "권", 2, 8_000L);
        PurchaseRequestProposalItem secondItem = new PurchaseRequestProposalItem("문구", null, 1, 4_000L);

        proposal.replaceBudget(budget);
        proposal.replaceItems(List.of(firstItem, secondItem));

        assertThat(proposal.getBudget()).isSameAs(budget);
        assertThat(budget.getProposal()).isSameAs(proposal);
        assertThat(proposal.getItems()).containsExactly(firstItem, secondItem);
        assertThat(firstItem.getProposal()).isSameAs(proposal);
        assertThat(firstItem.getSortOrder()).isZero();
        assertThat(firstItem.calculateExpectedAmount()).isEqualTo(16_000L);
        assertThat(secondItem.getSortOrder()).isEqualTo(1);
        assertThat(secondItem.calculateExpectedAmount()).isEqualTo(4_000L);
    }

    @Test
    void expectedAmount_isNullWhenQuantityOrUnitPriceIsMissing() {
        PurchaseRequestProposalItem missingQuantity = new PurchaseRequestProposalItem("교재", null, null, 5_000L);
        PurchaseRequestProposalItem missingUnitPrice = new PurchaseRequestProposalItem("교재", null, 1, null);

        assertThat(missingQuantity.calculateExpectedAmount()).isNull();
        assertThat(missingUnitPrice.calculateExpectedAmount()).isNull();
    }

    @Test
    void addReceipt_assignsOrderAndSoftDeleteKeepsHistory() {
        PurchaseRequestProposal proposal = new PurchaseRequestProposal(createPurchaseRequest());
        PurchaseRequestProposalReceipt firstReceipt = new PurchaseRequestProposalReceipt(mock(File.class));
        PurchaseRequestProposalReceipt secondReceipt = new PurchaseRequestProposalReceipt(mock(File.class));

        proposal.addReceipt(firstReceipt);
        proposal.addReceipt(secondReceipt);
        firstReceipt.softDelete();

        assertThat(proposal.getReceipts()).containsExactly(firstReceipt, secondReceipt);
        assertThat(firstReceipt.getProposal()).isSameAs(proposal);
        assertThat(firstReceipt.getSortOrder()).isZero();
        assertThat(firstReceipt.isDeleted()).isTrue();
        assertThat(firstReceipt.getDeletedAt()).isNotNull();
        assertThat(secondReceipt.getSortOrder()).isEqualTo(1);
        assertThat(secondReceipt.isDeleted()).isFalse();
    }

    @Test
    void receiptSoftDelete_isIdempotent() {
        PurchaseRequestProposalReceipt receipt = new PurchaseRequestProposalReceipt(mock(File.class));

        receipt.softDelete();
        var firstDeletedAt = receipt.getDeletedAt();
        receipt.softDelete();

        assertThat(receipt.isDeleted()).isTrue();
        assertThat(receipt.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    void enumDisplayNames_matchDocumentLabels() {
        assertThat(PurchasePaymentAccount.DISTRICT_BUDGET_01.getDisplayName()).isEqualTo("구비01");
        assertThat(PurchaseBudgetItemCategory.PROGRAM_OPERATION.getDisplayName()).isEqualTo("프로그램추진비");
        assertThat(PurchaseCalculationDetail.PRINTER_TONER.getDisplayName()).isEqualTo("프린트토너");
    }

    private PurchaseRequest createPurchaseRequest() {
        return new PurchaseRequest(
            mock(Classroom.class),
            null,
            mock(User.class),
            PurchasePaymentType.PREPAID,
            "교재 구매",
            null,
            List.of(new PurchaseRequestItem("교재", null, 1))
        );
    }
}
