package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyTeacherAttendanceCorrectionRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DailyTeacherAttendanceCorrectionValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validatesAbsentStatusWithoutTimes() {
        UpdateDailyTeacherAttendanceCorrectionRequest request = new UpdateDailyTeacherAttendanceCorrectionRequest(
            DailyTeacherAttendanceStatus.ABSENT,
            null,
            null
        );

        Set<ConstraintViolation<UpdateDailyTeacherAttendanceCorrectionRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validatesExcusedStatusWithoutTimes() {
        UpdateDailyTeacherAttendanceCorrectionRequest request = new UpdateDailyTeacherAttendanceCorrectionRequest(
            DailyTeacherAttendanceStatus.EXCUSED,
            null,
            null
        );

        Set<ConstraintViolation<UpdateDailyTeacherAttendanceCorrectionRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void rejectsAttendedAtWhenStatusIsAbsent() {
        UpdateDailyTeacherAttendanceCorrectionRequest request = new UpdateDailyTeacherAttendanceCorrectionRequest(
            DailyTeacherAttendanceStatus.ABSENT,
            LocalDateTime.of(2026, 6, 20, 14, 0),
            null
        );

        Set<ConstraintViolation<UpdateDailyTeacherAttendanceCorrectionRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(violation -> violation.getPropertyPath().toString().equals("attendedAt"));
    }

    @Test
    void rejectsAttendedAtWhenStatusIsExcused() {
        UpdateDailyTeacherAttendanceCorrectionRequest request = new UpdateDailyTeacherAttendanceCorrectionRequest(
            DailyTeacherAttendanceStatus.EXCUSED,
            LocalDateTime.of(2026, 6, 20, 14, 0),
            null
        );

        Set<ConstraintViolation<UpdateDailyTeacherAttendanceCorrectionRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(violation -> violation.getPropertyPath().toString().equals("attendedAt"));
    }

    @Test
    void rejectsMissingAttendedAtWhenStatusIsPresent() {
        UpdateDailyTeacherAttendanceCorrectionRequest request = new UpdateDailyTeacherAttendanceCorrectionRequest(
            DailyTeacherAttendanceStatus.PRESENT,
            null,
            null
        );

        Set<ConstraintViolation<UpdateDailyTeacherAttendanceCorrectionRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(violation -> violation.getPropertyPath().toString().equals("attendedAt"));
    }

    @Test
    void rejectsCheckOutTimeBeforeAttendanceTime() {
        UpdateDailyTeacherAttendanceCorrectionRequest request = new UpdateDailyTeacherAttendanceCorrectionRequest(
            DailyTeacherAttendanceStatus.PRESENT,
            LocalDateTime.of(2026, 6, 20, 14, 0),
            LocalDateTime.of(2026, 6, 20, 13, 50)
        );

        Set<ConstraintViolation<UpdateDailyTeacherAttendanceCorrectionRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(violation -> violation.getPropertyPath().toString().equals("checkedOutAt"));
    }
}
