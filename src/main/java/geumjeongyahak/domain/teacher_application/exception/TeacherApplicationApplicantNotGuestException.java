package geumjeongyahak.domain.teacher_application.exception;

import geumjeongyahak.common.exception.BusinessException;

public class TeacherApplicationApplicantNotGuestException extends BusinessException {

    public TeacherApplicationApplicantNotGuestException() {
        super(TeacherApplicationErrorCode.TEACHER_APPLICATION_APPLICANT_NOT_GUEST);
    }
}
