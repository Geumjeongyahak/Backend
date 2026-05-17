package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidDailyScheduleRange;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailyScheduleListRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DailyScheduleRangeValidator implements ConstraintValidator<ValidDailyScheduleRange, DailyScheduleListRequest> {

    private static final long MAX_RANGE_DAYS = 42;

    @Override
    public boolean isValid(DailyScheduleListRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDate from = value.from();
        LocalDate to = value.to();
        if (from == null || to == null) {
            return true;
        }

        if (from.isAfter(to)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("시작 일자는 종료 일자보다 이후일 수 없습니다.")
                .addPropertyNode("from")
                .addConstraintViolation();
            return false;
        }

        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("날짜 범위가 너무 넓습니다.")
                .addConstraintViolation();
            return false;
        }

        return true;
    }
}
