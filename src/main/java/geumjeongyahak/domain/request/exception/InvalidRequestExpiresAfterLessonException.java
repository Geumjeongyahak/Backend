package geumjeongyahak.domain.request.exception;

import geumjeongyahak.common.exception.BusinessException;

public class InvalidRequestExpiresAfterLessonException extends BusinessException {

    public InvalidRequestExpiresAfterLessonException() {
        super(RequestErrorCode.INVALID_REQUEST_EXPIRES_AFTER_LESSON);
    }
}
