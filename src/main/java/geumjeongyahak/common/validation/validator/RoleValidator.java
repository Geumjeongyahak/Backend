package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidRole;
import geumjeongyahak.domain.auth.enums.RoleType;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RoleValidator implements ConstraintValidator<ValidRole, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            RoleType.valueOf(value);
            return true; 
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
