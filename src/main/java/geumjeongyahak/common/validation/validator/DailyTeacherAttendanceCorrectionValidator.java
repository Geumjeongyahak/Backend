package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidDailyTeacherAttendanceCorrection;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyTeacherAttendanceCorrectionRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DailyTeacherAttendanceCorrectionValidator
    implements ConstraintValidator<ValidDailyTeacherAttendanceCorrection, UpdateDailyTeacherAttendanceCorrectionRequest> {

    @Override
    public boolean isValid(UpdateDailyTeacherAttendanceCorrectionRequest value, ConstraintValidatorContext context) {
        if (value == null || value.status() == null) {
            return true;
        }

        if (isActualAbsence(value.status()) && (value.attendedAt() != null || value.checkedOutAt() != null)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("결석 또는 공결 상태에서는 출근 시간과 퇴근 시간을 입력할 수 없습니다.")
                .addPropertyNode(value.attendedAt() != null ? "attendedAt" : "checkedOutAt")
                .addConstraintViolation();
            return false;
        }

        if (!isActualAbsence(value.status()) && value.attendedAt() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("출석 상태에서는 출근 시간을 입력해야 합니다.")
                .addPropertyNode("attendedAt")
                .addConstraintViolation();
            return false;
        }

        if (value.attendedAt() != null
            && value.checkedOutAt() != null
            && value.checkedOutAt().isBefore(value.attendedAt())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("퇴근 시간이 출근 시간보다 빠를 수 없습니다.")
                .addPropertyNode("checkedOutAt")
                .addConstraintViolation();
            return false;
        }

        return true;
    }

    private boolean isActualAbsence(DailyTeacherAttendanceStatus status) {
        return status == DailyTeacherAttendanceStatus.ABSENT || status == DailyTeacherAttendanceStatus.EXCUSED;
    }
}
