package sonmoeum.common.validation.validator;

import java.util.List;

import sonmoeum.common.validation.annotation.ValidPermissions;
import sonmoeum.domain.auth.enums.PermissionType;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PermissionsValidator implements ConstraintValidator<ValidPermissions, List<String>> {

    @Override
    public boolean isValid(List<String> values, ConstraintValidatorContext context) {
        if (values == null || values.isEmpty()) {
            return true; // Empty list is valid, distinct from null checking
        }
        for (String value : values) {
            try {
                PermissionType.valueOf(value);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }
}
