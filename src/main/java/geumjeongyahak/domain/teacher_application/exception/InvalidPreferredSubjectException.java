package geumjeongyahak.domain.teacher_application.exception;

import geumjeongyahak.common.exception.BusinessException;

public class InvalidPreferredSubjectException extends BusinessException {

    public InvalidPreferredSubjectException() {
        super(TeacherApplicationErrorCode.INVALID_PREFERRED_SUBJECT);
    }
}
