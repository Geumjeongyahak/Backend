package geumjeongyahak.common.validation.annotation;

import geumjeongyahak.common.validation.validator.LessonExchangeProposalInputValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LessonExchangeProposalInputValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLessonExchangeProposalInput {
    String message() default "유효하지 않은 수업 교환 제안 입력입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
