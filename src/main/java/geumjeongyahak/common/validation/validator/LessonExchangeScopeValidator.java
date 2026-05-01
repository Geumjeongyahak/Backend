package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidLessonExchangeScope;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeRequestRequest;
import geumjeongyahak.domain.request.v1.dto.request.UpdateLessonExchangeRequestRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LessonExchangeScopeValidator
    implements ConstraintValidator<ValidLessonExchangeScope, Object> {

    private static final int MIN_PERIOD = 1;
    private static final int MAX_PERIOD = 3;

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        Integer startPeriod;
        Integer endPeriod;

        if (value instanceof CreateLessonExchangeRequestRequest request) {
            startPeriod = request.startPeriod();
            endPeriod = request.endPeriod();
        } else if (value instanceof UpdateLessonExchangeRequestRequest request) {
            startPeriod = request.startPeriod();
            endPeriod = request.endPeriod();
        } else {
            return true;
        }

        if (startPeriod == null && endPeriod == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        if ((startPeriod == null) != (endPeriod == null)) {
            context.buildConstraintViolationWithTemplate("시작 교시와 종료 교시는 함께 입력해야 합니다.")
                .addPropertyNode("startPeriod")
                .addConstraintViolation();
            return false;
        }

        if (startPeriod < MIN_PERIOD || startPeriod > MAX_PERIOD) {
            context.buildConstraintViolationWithTemplate("시작 교시는 1교시부터 3교시 사이여야 합니다.")
                .addPropertyNode("startPeriod")
                .addConstraintViolation();
            return false;
        }

        if (endPeriod < MIN_PERIOD || endPeriod > MAX_PERIOD) {
            context.buildConstraintViolationWithTemplate("종료 교시는 1교시부터 3교시 사이여야 합니다.")
                .addPropertyNode("endPeriod")
                .addConstraintViolation();
            return false;
        }

        if (startPeriod > endPeriod) {
            context.buildConstraintViolationWithTemplate("시작 교시는 종료 교시보다 이후일 수 없습니다.")
                .addPropertyNode("startPeriod")
                .addConstraintViolation();
            return false;
        }

        return true;
    }
}
