package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidDailyTeacherAttendanceLocation;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyTeacherAttendanceRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DailyTeacherAttendanceLocationValidator
    implements ConstraintValidator<ValidDailyTeacherAttendanceLocation, UpdateDailyTeacherAttendanceRequest> {

    @Override
    public boolean isValid(UpdateDailyTeacherAttendanceRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean hasLatitude = value.latitude() != null;
        boolean hasLongitude = value.longitude() != null;
        if (hasLatitude != hasLongitude) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("위도와 경도는 함께 입력해야 합니다.")
                .addPropertyNode(hasLatitude ? "longitude" : "latitude")
                .addConstraintViolation();
            return false;
        }

        if (value.status() == DailyTeacherAttendanceStatus.ABSENT && hasLatitude) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("결석 상태에서는 위치 정보를 입력할 수 없습니다.")
                .addPropertyNode("status")
                .addConstraintViolation();
            return false;
        }

        return true;
    }
}
