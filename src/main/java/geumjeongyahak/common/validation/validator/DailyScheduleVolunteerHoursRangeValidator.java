package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidDailyScheduleVolunteerHoursRange;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.DailyScheduleVolunteerHoursRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class DailyScheduleVolunteerHoursRangeValidator
    implements ConstraintValidator<ValidDailyScheduleVolunteerHoursRange, DailyScheduleVolunteerHoursRequest> {

    @Override
    public boolean isValid(DailyScheduleVolunteerHoursRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDate from = value.from();
        LocalDate to = value.to();
        if (from == null || to == null || !from.isAfter(to)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("시작 일자는 종료 일자보다 이후일 수 없습니다.")
            .addPropertyNode("from")
            .addConstraintViolation();
        return false;
    }
}
