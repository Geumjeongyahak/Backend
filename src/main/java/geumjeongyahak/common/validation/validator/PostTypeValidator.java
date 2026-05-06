package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidPostType;
import geumjeongyahak.domain.post.enums.PostType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PostTypeValidator implements ConstraintValidator<ValidPostType, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        try {
            PostType.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
