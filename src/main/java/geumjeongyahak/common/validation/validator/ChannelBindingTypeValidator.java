package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidChannelBindingType;
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ChannelBindingTypeValidator implements ConstraintValidator<ValidChannelBindingType, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            ChannelBindingType.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
