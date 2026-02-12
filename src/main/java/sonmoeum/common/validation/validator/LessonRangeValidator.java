package sonmoeum.common.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import sonmoeum.common.validation.annotation.ValidLessonRange;
import sonmoeum.domain.lesson.v1.dto.request.LessonRangeRequest;

public class LessonRangeValidator implements ConstraintValidator<ValidLessonRange, LessonRangeRequest> {

    private static final long MAX_RANGE_DAYS = 31;

    @Override
    public boolean isValid(LessonRangeRequest value, ConstraintValidatorContext context) {
        if (value == null) return true;

        LocalDate from = value.from();
        LocalDate to = value.to();

        if (from == null || to == null) return true;

        // from <= to
        if (from.isAfter(to)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("시작 일자는 종료 일자보다 이후일 수 없습니다.")
                .addPropertyNode("from")
                .addConstraintViolation();
            return false;
        }

        // range <= MAX_RANGE_DAYS
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_RANGE_DAYS) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("날짜 범위가 너무 넓습니다.")
                .addConstraintViolation();
            return false;
        }

        return true;
    }
}
