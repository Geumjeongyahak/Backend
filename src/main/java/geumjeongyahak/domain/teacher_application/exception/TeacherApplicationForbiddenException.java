package geumjeongyahak.domain.teacher_application.exception;

import geumjeongyahak.common.exception.BusinessException;

public class TeacherApplicationForbiddenException extends BusinessException {

    public TeacherApplicationForbiddenException() {
        super(TeacherApplicationErrorCode.TEACHER_APPLICATION_FORBIDDEN);
    }
}
