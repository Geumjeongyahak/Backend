package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidPermissionCode;
import geumjeongyahak.domain.base.model.PermissionCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PermissionCodeValidator implements ConstraintValidator<ValidPermissionCode, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        try {
            new PermissionCode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
