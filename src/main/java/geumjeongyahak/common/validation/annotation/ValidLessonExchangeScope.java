package geumjeongyahak.common.validation.annotation;

import geumjeongyahak.common.validation.validator.LessonExchangeScopeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LessonExchangeScopeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLessonExchangeScope {
    String message() default "유효하지 않은 수업 교환 범위입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
