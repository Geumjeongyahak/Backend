package sonmoeum.common.validation.validator;

import sonmoeum.common.validation.annotation.ValidRole;
import sonmoeum.domain.auth.enums.RoleLevel;
import sonmoeum.domain.auth.enums.RoleType;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RoleValidator implements ConstraintValidator<ValidRole, String> {
    private Set<RoleLevel> validLevels;

    @Override
    public void initialize(ValidRole constraintAnnotation) {
        validLevels = Arrays.stream(constraintAnnotation.levels())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Let @NotNull handle empty values
        }
        try {
            return validLevels.contains(RoleType.valueOf(value).getLevel());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
