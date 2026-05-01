package geumjeongyahak.common.validation.annotation;

import geumjeongyahak.common.validation.validator.PermissionCodeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PermissionCodeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPermissionCode {
    String message() default "유효하지 않은 권한 코드입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
