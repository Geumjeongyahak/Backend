package geumjeongyahak.common.validation.annotation;

import geumjeongyahak.common.validation.validator.DailyScheduleVolunteerHoursRangeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = DailyScheduleVolunteerHoursRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDailyScheduleVolunteerHoursRange {
    String message() default "잘못된 봉사 시간 조회 날짜 범위입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
