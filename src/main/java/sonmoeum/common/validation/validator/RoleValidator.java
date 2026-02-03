package sonmoeum.common.validation.validator;

import sonmoeum.common.validation.annotation.ValidRole;
import sonmoeum.domain.auth.enums.RoleType;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RoleValidator implements ConstraintValidator<ValidRole, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Let @NotNull handle empty values
        }
        try {
            RoleType.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
