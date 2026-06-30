package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.UpdateDailyTeacherAttendanceRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DailyTeacherAttendanceLocationValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsLocationWhenStatusIsExcused() {
        UpdateDailyTeacherAttendanceRequest request = new UpdateDailyTeacherAttendanceRequest(
            DailyTeacherAttendanceStatus.EXCUSED,
            new BigDecimal("35.1795543"),
            new BigDecimal("129.0756416")
        );

        Set<ConstraintViolation<UpdateDailyTeacherAttendanceRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(violation -> violation.getPropertyPath().toString().equals("status"));
    }
}
