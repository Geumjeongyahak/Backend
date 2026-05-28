package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidEventSearchCondition;
import geumjeongyahak.domain.event.v1.dto.request.EventSearchRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EventSearchConditionValidator implements ConstraintValidator<ValidEventSearchCondition, EventSearchRequest> {

    @Override
    public boolean isValid(EventSearchRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean hasStart = value.getStartDate() != null;
        boolean hasEnd = value.getEndDate() != null;

        if (hasStart != hasEnd) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("startDate와 endDate는 함께 전달해야 합니다.")
                .addPropertyNode(hasStart ? "endDate" : "startDate")
                .addConstraintViolation();
            return false;
        }

        if (hasStart && value.getStartDate().isAfter(value.getEndDate())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("startDate는 endDate보다 늦을 수 없습니다.")
                .addPropertyNode("startDate")
                .addConstraintViolation();
            return false;
        }

        return true;
    }
}
