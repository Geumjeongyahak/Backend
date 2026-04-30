package geumjeongyahak.common.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

import geumjeongyahak.common.validation.validator.ChannelTypeValidator;

@Documented
@Constraint(validatedBy = ChannelTypeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidChannelType {
    String message() default "유효하지 않은 채널 유형입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
