package geumjeongyahak.unit.purchase_request;

import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.domain.purchase_request.enums.PurchaseBudgetItemCategory;
import geumjeongyahak.domain.purchase_request.enums.PurchaseCalculationDetail;
import geumjeongyahak.domain.purchase_request.v1.dto.request.SavePurchaseRequestProposalRequest.BudgetRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PurchaseProposalBudgetValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void allowsDirectInputWithCustomValues() {
        BudgetRequest request = new BudgetRequest(
            PurchaseBudgetItemCategory.DIRECT_INPUT,
            "시설 유지보수비",
            PurchaseCalculationDetail.DIRECT_INPUT,
            "냉난방기 수리"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsDirectInputWithoutCustomValue() {
        BudgetRequest request = new BudgetRequest(
            PurchaseBudgetItemCategory.DIRECT_INPUT,
            " ",
            PurchaseCalculationDetail.COMMERCIAL_TEXTBOOK,
            null
        );

        Set<ConstraintViolation<BudgetRequest>> violations = validator.validate(request);

        assertThat(violations)
            .anyMatch(violation -> violation.getPropertyPath().toString().equals("customItemCategory"));
    }

    @Test
    void rejectsCustomValueForPredefinedSelection() {
        BudgetRequest request = new BudgetRequest(
            PurchaseBudgetItemCategory.TEXTBOOK,
            "임의 항목",
            PurchaseCalculationDetail.COMMERCIAL_TEXTBOOK,
            null
        );

        Set<ConstraintViolation<BudgetRequest>> violations = validator.validate(request);

        assertThat(violations)
            .anyMatch(violation -> violation.getPropertyPath().toString().equals("customItemCategory"));
    }

    @Test
    void allowsEmptyOptionalBudget() {
        BudgetRequest request = new BudgetRequest(null, null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void validatesItemCategoryAndCalculationDetailTogether() {
        BudgetRequest request = new BudgetRequest(
            PurchaseBudgetItemCategory.DIRECT_INPUT,
            null,
            PurchaseCalculationDetail.DIRECT_INPUT,
            null
        );

        Set<ConstraintViolation<BudgetRequest>> violations = validator.validate(request);

        assertThat(violations)
            .extracting(violation -> violation.getPropertyPath().toString())
            .containsExactlyInAnyOrder("customItemCategory", "customCalculationDetail");
    }
}
