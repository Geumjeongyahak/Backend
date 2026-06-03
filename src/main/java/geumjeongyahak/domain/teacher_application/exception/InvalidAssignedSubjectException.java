package geumjeongyahak.domain.teacher_application.exception;

import geumjeongyahak.common.exception.BusinessException;

public class InvalidAssignedSubjectException extends BusinessException {

    public InvalidAssignedSubjectException() {
        super(TeacherApplicationErrorCode.INVALID_ASSIGNED_SUBJECT);
    }
}
