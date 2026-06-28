package geumjeongyahak.common.validation.annotation;

import geumjeongyahak.common.validation.validator.DailyTeacherAttendanceCorrectionValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = DailyTeacherAttendanceCorrectionValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDailyTeacherAttendanceCorrection {
    String message() default "잘못된 교사 출석 보정 정보입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
