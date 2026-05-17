package geumjeongyahak.common.validation.annotation;

import geumjeongyahak.common.validation.validator.DailyScheduleRangeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = DailyScheduleRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDailyScheduleRange {
    String message() default "잘못된 하루 일정 날짜 범위입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
