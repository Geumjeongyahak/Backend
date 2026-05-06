package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidChannelManagementMode;
import geumjeongyahak.domain.channel.enums.ChannelManagementMode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ChannelManagementModeValidator implements ConstraintValidator<ValidChannelManagementMode, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        try {
            ChannelManagementMode.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
