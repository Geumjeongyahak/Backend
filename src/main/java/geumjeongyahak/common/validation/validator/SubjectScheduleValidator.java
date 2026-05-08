package geumjeongyahak.common.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.LocalTime;
import geumjeongyahak.common.validation.annotation.ValidSubjectSchedule;
import geumjeongyahak.domain.subject.v1.dto.request.CreateSubjectRequest;

public class SubjectScheduleValidator implements ConstraintValidator<ValidSubjectSchedule, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDate startAt;
        LocalDate endAt;
        LocalDate assignedFrom;
        LocalDate assignedTo;
        LocalTime startTime;
        LocalTime endTime;

        if (value instanceof CreateSubjectRequest req) {
            startAt = req.startAt();
            endAt = req.endAt();
            assignedFrom = req.assignedFrom();
            assignedTo = req.assignedTo();
            startTime = req.startTime();
            endTime = req.endTime();
        } else {
            return true;
        }

        // startAt <= endAt
        boolean dateCheck = startAt != null && endAt != null && startAt.isAfter(endAt);
        // startTime < endTime
        boolean timeCheck = startTime != null && endTime != null && !startTime.isBefore(endTime);
        // assignedFrom <= assignedTo
        boolean assignmentDateCheck = assignedFrom != null && assignedTo != null && assignedFrom.isAfter(assignedTo);

        if (!dateCheck && !timeCheck && !assignmentDateCheck) return true;

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

        if (assignmentDateCheck) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("assignedFrom은 assignedTo보다 늦을 수 없습니다.")
                .addPropertyNode("assignedFrom")
                .addConstraintViolation();
            return false;
        }

        return true;
    }
}
