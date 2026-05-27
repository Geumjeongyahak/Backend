package geumjeongyahak.common.validation.validator;

import java.time.LocalTime;

import geumjeongyahak.common.validation.annotation.ValidEventTime;
import geumjeongyahak.domain.event.v1.dto.request.CreateEventRequest;
import geumjeongyahak.domain.event.v1.dto.request.UpdateEventRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EventTimeValidator implements ConstraintValidator<ValidEventTime, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalTime startTime;
        LocalTime endTime;

        if (value instanceof CreateEventRequest request) {
            startTime = request.startTime();
            endTime = request.endTime();
        } else if (value instanceof UpdateEventRequest request) {
            startTime = request.startTime();
            endTime = request.endTime();
        } else {
            return true;
        }

        if (startTime == null && endTime == null) {
            return true;
        }

        if (startTime == null || endTime == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("startTime과 endTime은 함께 전달해야 합니다.")
                .addPropertyNode(startTime == null ? "startTime" : "endTime")
                .addConstraintViolation();
            return false;
        }

        if (!endTime.isBefore(startTime)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("endTime은 startTime보다 빠를 수 없습니다.")
            .addPropertyNode("endTime")
            .addConstraintViolation();
        return false;
    }
}
