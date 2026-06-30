package geumjeongyahak.common.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import geumjeongyahak.common.validation.validator.ClassroomTypeValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ClassroomTypeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidClassroomType {
    String message() default "유효하지 않은 교실 유형입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
