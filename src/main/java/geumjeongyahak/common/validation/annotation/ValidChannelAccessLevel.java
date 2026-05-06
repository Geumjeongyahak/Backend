package geumjeongyahak.common.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import geumjeongyahak.common.validation.validator.ChannelAccessLevelValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ChannelAccessLevelValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidChannelAccessLevel {
    String message() default "유효하지 않은 채널 접근 수준입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
