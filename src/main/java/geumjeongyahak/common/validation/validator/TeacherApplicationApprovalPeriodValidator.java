package geumjeongyahak.common.validation.validator;

import geumjeongyahak.common.validation.annotation.ValidTeacherApplicationApprovalPeriod;
import geumjeongyahak.domain.teacher_application.v1.dto.request.ApproveTeacherApplicationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class TeacherApplicationApprovalPeriodValidator
    implements ConstraintValidator<ValidTeacherApplicationApprovalPeriod, ApproveTeacherApplicationRequest> {

    @Override
    public boolean isValid(ApproveTeacherApplicationRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDate teacherStartAt = value.teacherStartAt();
        LocalDate teacherEndAt = value.teacherEndAt();
        if (teacherStartAt == null || teacherEndAt == null) {
            return true;
        }

        if (teacherEndAt.isBefore(teacherStartAt)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("교원 활동 종료일은 시작일보다 빠를 수 없습니다.")
                .addPropertyNode("teacherEndAt")
                .addConstraintViolation();
            return false;
        }

        return true;
    }
}
