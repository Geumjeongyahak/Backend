package sonmoeum.common.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.LocalTime;
import sonmoeum.common.validation.annotation.ValidSubjectSchedule;
import sonmoeum.domain.subject.v1.dto.request.CreateSubjectRequest;

public class SubjectScheduleValidator implements ConstraintValidator<ValidSubjectSchedule, CreateSubjectRequest> {

    @Override
    public boolean isValid(CreateSubjectRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        // 단일 필드 @NotNull 등이 먼저 잡도록 구현, null이면 여기서는 통과
        LocalDate startAt = value.startAt();
        LocalDate endAt = value.endAt();
        LocalTime startTime = value.startTime();
        LocalTime endTime = value.endTime();

        // startAt <= endAt
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("startAt은 endAt보다 늦을 수 없습니다.")
                .addPropertyNode("startAt")
                .addConstraintViolation();
            return false;
        }

        // startTime < endTime
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("startTime은 endTime보다 빨라야 합니다.")
                .addPropertyNode("startTime")
                .addConstraintViolation();
            return false;
        }

        return true;
    }
}
