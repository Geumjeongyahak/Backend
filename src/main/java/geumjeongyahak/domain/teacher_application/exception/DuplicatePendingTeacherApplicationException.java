package geumjeongyahak.domain.teacher_application.exception;

import geumjeongyahak.common.exception.BusinessException;

public class DuplicatePendingTeacherApplicationException extends BusinessException {

    public DuplicatePendingTeacherApplicationException() {
        super(TeacherApplicationErrorCode.TEACHER_APPLICATION_ALREADY_PENDING);
    }
}
