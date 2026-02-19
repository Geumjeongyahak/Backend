package sonmoeum.common.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.LocalTime;
import sonmoeum.common.validation.annotation.ValidSubjectSchedule;
import sonmoeum.domain.subject.v1.dto.request.CreateSubjectRequest;
import sonmoeum.domain.subject.v1.dto.request.UpdateSubjectRequest;

public class SubjectScheduleValidator implements ConstraintValidator<ValidSubjectSchedule, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDate startAt;
        LocalDate endAt;
        LocalTime startTime;
        LocalTime endTime;

        if (value instanceof CreateSubjectRequest req) {
            startAt = req.startAt();
            endAt = req.endAt();
            startTime = req.startTime();
            endTime = req.endTime();
        } else if (value instanceof UpdateSubjectRequest req) {
            startAt = req.startAt();
            endAt = req.endAt();
            startTime = req.startTime();
            endTime = req.endTime();
        } else {
            return true;
        }

        // startAt <= endAt
        boolean dateCheck = startAt != null && endAt != null && startAt.isAfter(endAt);
        // startTime < endTime
        boolean timeCheck = startTime != null && endTime != null && !startTime.isBefore(endTime);

        if (!dateCheck && !timeCheck) return true;

        if (dateCheck) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("startAt은 endAt보다 늦을 수 없습니다.")
                .addPropertyNode("startAt")
                .addConstraintViolation();
            return false;
        }

        if (timeCheck) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("startTime은 endTime보다 빨라야 합니다.")
                .addPropertyNode("startTime")
                .addConstraintViolation();
            return false;
        }

        return true;
    }
}
