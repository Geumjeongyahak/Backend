package sonmoeum.common.validation.annotation;

import jakarta.validation.Constraint;
import sonmoeum.common.validation.validator.SortFieldValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SortFieldValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSortField {
    String message() default "유효하지 않은 정렬 기준입니다.'필드명1,ASC|DESC;필드명2,ASC|DESC' 형식으로 입력해주세요.";
    String[] fields() default {};
    Class<?>[] groups() default {};
    Class<? extends jakarta.validation.Payload>[] payload() default {};

}
