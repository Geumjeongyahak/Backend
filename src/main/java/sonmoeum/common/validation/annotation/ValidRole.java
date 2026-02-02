package sonmoeum.common.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import sonmoeum.common.validation.validator.RoleValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = RoleValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRole {
    String message() default "유효하지 않은 역할(Role) 입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
