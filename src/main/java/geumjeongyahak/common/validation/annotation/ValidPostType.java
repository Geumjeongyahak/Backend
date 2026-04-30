package geumjeongyahak.common.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import geumjeongyahak.common.validation.validator.PostTypeValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PostTypeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPostType {
    String message() default "유효하지 않은 게시글 유형입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
