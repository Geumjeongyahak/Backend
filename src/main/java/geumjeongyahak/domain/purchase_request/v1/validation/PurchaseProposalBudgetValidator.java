package geumjeongyahak.domain.purchase_request.v1.validation;

import geumjeongyahak.domain.purchase_request.enums.PurchaseBudgetItemCategory;
import geumjeongyahak.domain.purchase_request.enums.PurchaseCalculationDetail;
import geumjeongyahak.domain.purchase_request.v1.dto.request.SavePurchaseRequestProposalRequest.BudgetRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class PurchaseProposalBudgetValidator
    implements ConstraintValidator<ValidPurchaseProposalBudget, BudgetRequest> {

    @Override
    public boolean isValid(BudgetRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        boolean itemCategoryValid = validateCustomValue(
            value.itemCategory() == PurchaseBudgetItemCategory.DIRECT_INPUT,
            value.customItemCategory(),
            "customItemCategory",
            "세부 항목에서 직접 입력을 선택한 경우 직접 입력값이 필요합니다.",
            context
        );
        boolean calculationDetailValid = validateCustomValue(
            value.calculationDetail() == PurchaseCalculationDetail.DIRECT_INPUT,
            value.customCalculationDetail(),
            "customCalculationDetail",
            "산출 내역에서 직접 입력을 선택한 경우 직접 입력값이 필요합니다.",
            context
        );

        return itemCategoryValid && calculationDetailValid;
    }

    private boolean validateCustomValue(
        boolean directInput,
        String customValue,
        String property,
        String requiredMessage,
        ConstraintValidatorContext context
    ) {
        boolean hasCustomValue = StringUtils.hasText(customValue);
        if (directInput == hasCustomValue) {
            return true;
        }

        String message = directInput
            ? requiredMessage
            : "직접 입력값은 직접 입력을 선택한 경우에만 입력할 수 있습니다.";
        context.buildConstraintViolationWithTemplate(message)
            .addPropertyNode(property)
            .addConstraintViolation();
        return false;
    }
}
