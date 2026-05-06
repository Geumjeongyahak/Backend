package geumjeongyahak.common.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import geumjeongyahak.common.validation.validator.PostStatusValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PostStatusValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPostStatus {
    String message() default "유효하지 않은 게시글 상태입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
