package sonmoeum.common.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import sonmoeum.common.validation.validator.PermissionsValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = PermissionsValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPermissions {
    String message() default "유효하지 않은 권한(Permission)이 포함되어 있습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
