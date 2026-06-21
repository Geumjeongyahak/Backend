package geumjeongyahak.common.validation.annotation;

import geumjeongyahak.common.validation.validator.UserBirthDateValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = UserBirthDateValidator.class)
@Target({
    ElementType.FIELD,
    ElementType.PARAMETER,
    ElementType.RECORD_COMPONENT,
    ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUserBirthDate {

    String message() default "유효하지 않은 생년월일입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
