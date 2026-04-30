package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidPostStatus;
import geumjeongyahak.domain.post.enums.PostStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PostStatusValidator implements ConstraintValidator<ValidPostStatus, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        try {
            PostStatus.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
