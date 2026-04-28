package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidLessonExchangeScope;
import geumjeongyahak.domain.request.enums.LessonExchangeScope;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeRequestRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LessonExchangeScopeValidator
    implements ConstraintValidator<ValidLessonExchangeScope, CreateLessonExchangeRequestRequest> {

    @Override
    public boolean isValid(CreateLessonExchangeRequestRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LessonExchangeScope scope = value.scope();
        Integer startPeriod = value.startPeriod();
        Integer endPeriod = value.endPeriod();

        if (scope == null) {
            return true;
        }

        if (scope == LessonExchangeScope.FULL) {
            if (startPeriod != null || endPeriod != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("전체 교환은 시작 교시와 종료 교시를 입력할 수 없습니다.")
                    .addPropertyNode("startPeriod")
                    .addConstraintViolation();
                return false;
            }
            return true;
        }

        if (scope == LessonExchangeScope.PARTIAL) {
            if (startPeriod == null || endPeriod == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("부분 교환은 시작 교시와 종료 교시가 모두 필요합니다.")
                    .addPropertyNode("startPeriod")
                    .addConstraintViolation();
                return false;
            }

            if (startPeriod < 1 || startPeriod > 3) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("시작 교시는 1교시부터 3교시 사이여야 합니다.")
                    .addPropertyNode("startPeriod")
                    .addConstraintViolation();
                return false;
            }

            if (endPeriod < 1 || endPeriod > 3) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("종료 교시는 1교시부터 3교시 사이여야 합니다.")
                    .addPropertyNode("endPeriod")
                    .addConstraintViolation();
                return false;
            }

            if (startPeriod > endPeriod) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("시작 교시는 종료 교시보다 이후일 수 없습니다.")
                    .addPropertyNode("startPeriod")
                    .addConstraintViolation();
                return false;
            }

            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("유효하지 않은 교환 범위입니다.")
            .addPropertyNode("scope")
            .addConstraintViolation();
        return false;
    }
}
