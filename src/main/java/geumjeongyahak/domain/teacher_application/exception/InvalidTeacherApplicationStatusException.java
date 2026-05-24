package geumjeongyahak.domain.teacher_application.exception;

import geumjeongyahak.common.exception.BusinessException;

public class InvalidTeacherApplicationStatusException extends BusinessException {

    public InvalidTeacherApplicationStatusException() {
        super(TeacherApplicationErrorCode.INVALID_TEACHER_APPLICATION_STATUS);
    }
}
