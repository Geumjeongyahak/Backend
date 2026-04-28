package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidLessonExchangeProposalInput;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeProposalRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class LessonExchangeProposalInputValidator
    implements ConstraintValidator<ValidLessonExchangeProposalInput, CreateLessonExchangeProposalRequest> {

    private static final int MIN_PERIOD = 1;
    private static final int MAX_PERIOD = 3;

    @Override
    public boolean isValid(
        CreateLessonExchangeProposalRequest value,
        ConstraintValidatorContext context
    ) {
        if (value == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        LocalDate lessonDate = value.lessonDate();
        Integer startPeriod = value.startPeriod();
        Integer endPeriod = value.endPeriod();

        // lessonDate가 없으면 대체형 제안
        if (lessonDate == null) {
            if (startPeriod != null || endPeriod != null) {
                context.buildConstraintViolationWithTemplate(
                        "수업 일자를 입력하지 않는 대체형 제안은 시작/종료 교시를 입력할 수 없습니다."
                    )
                    .addConstraintViolation();
                return false;
            }

            return true;
        }

        // lessonDate가 있으면 교환형 제안
        if ((startPeriod == null) != (endPeriod == null)) {
            context.buildConstraintViolationWithTemplate("시작 교시와 종료 교시는 함께 입력해야 합니다.")
                .addConstraintViolation();
            return false;
        }

        if (startPeriod != null && endPeriod != null) {
            if (startPeriod < MIN_PERIOD || endPeriod > MAX_PERIOD || startPeriod > endPeriod) {
                context.buildConstraintViolationWithTemplate("유효하지 않은 교시 범위입니다.")
                    .addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
