package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidPhoneNumber;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // null or empty is allowed, use @NotBlank if required
        }
        return value.matches("^\\d{2,3}-\\d{3,4}-\\d{4}$");
    }
}
