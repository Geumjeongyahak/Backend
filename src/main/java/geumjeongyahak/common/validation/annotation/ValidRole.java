package geumjeongyahak.common.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import geumjeongyahak.common.validation.validator.RoleValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import geumjeongyahak.domain.auth.enums.RoleLevel;

@Documented
@Constraint(validatedBy = RoleValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRole {
    String message() default "유효하지 않은 역할(Role) 입니다.";
    RoleLevel[] levels() default { RoleLevel.BASIC, RoleLevel.DEPARTMENT, RoleLevel.ADDITIONAL };
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
