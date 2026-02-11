package sonmoeum.common.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import sonmoeum.common.validation.validator.LessonRangeValidator;

@Documented
@Constraint(validatedBy = LessonRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLessonRange {
    String message() default "잘못된 날짜 범위입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
