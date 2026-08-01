package geumjeongyahak.domain.purchase_request.v1.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PurchaseRequestTargetValidator
    implements ConstraintValidator<ValidPurchaseRequestTarget, PurchaseRequestTarget> {

    @Override
    public boolean isValid(PurchaseRequestTarget value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean hasClassroom = value.classroomId() != null;
        boolean hasDepartment = value.departmentId() != null;
        if (hasClassroom != hasDepartment) {
            return true;
        }

        String messageTemplate = context.getDefaultConstraintMessageTemplate();
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(messageTemplate)
            .addPropertyNode("classroomId")
            .addConstraintViolation();
        return false;
    }
}
