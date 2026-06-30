package geumjeongyahak.common.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import geumjeongyahak.common.validation.annotation.ValidClassroomType;
import geumjeongyahak.domain.classroom.enums.ClassroomType;

public class ClassroomTypeValidator implements ConstraintValidator<ValidClassroomType, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        try {
            ClassroomType.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
