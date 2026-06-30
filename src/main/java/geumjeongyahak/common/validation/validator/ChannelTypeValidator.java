package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidChannelType;
import geumjeongyahak.domain.channel.enums.ChannelType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ChannelTypeValidator implements ConstraintValidator<ValidChannelType, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        try {
            ChannelType.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
