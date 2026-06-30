package geumjeongyahak.common.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalTime;
import geumjeongyahak.common.validation.annotation.ValidLessonTime;
import geumjeongyahak.domain.lesson.v1.dto.request.CreateLessonRequest;

public class LessonTimeValidator implements ConstraintValidator<ValidLessonTime, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalTime startTime;
        LocalTime endTime;

        if (value instanceof CreateLessonRequest req) {
            startTime = req.startTime();
            endTime = req.endTime();
        } else {
            return true;
        }

        // null 검증은 @NotNull에 맡김
        if (startTime == null || endTime == null) {
            return true;
        }

        // startTime < endTime
        boolean invalid = !startTime.isBefore(endTime);
        if (!invalid) return true;

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("startTime은 endTime보다 빨라야 합니다.")
            .addPropertyNode("startTime")
            .addConstraintViolation();

        return false;
    }
}