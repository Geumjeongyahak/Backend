package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidUserBirthDate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class UserBirthDateValidator
    implements ConstraintValidator<ValidUserBirthDate, LocalDate> {

    private static final int MAX_AGE_EXCLUSIVE = 100;

    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
        if (birthDate == null) {
            return true;
        }

        LocalDate referenceDate = LocalDate.now();
        if (birthDate.isAfter(referenceDate)) {
            return violation(context, "생년월일은 미래일 수 없습니다.");
        }
        if (!birthDate.isAfter(referenceDate.minusYears(MAX_AGE_EXCLUSIVE))) {
            return violation(context, "생년월일은 만 100세 미만이어야 합니다.");
        }
        return true;
    }

    private boolean violation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
            .addConstraintViolation();
        return false;
    }
}
