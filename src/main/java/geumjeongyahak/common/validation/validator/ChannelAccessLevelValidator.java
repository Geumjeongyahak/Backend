package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ChannelAccessLevelValidator implements ConstraintValidator<ValidChannelAccessLevel, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        try {
            ChannelAccessLevel.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
