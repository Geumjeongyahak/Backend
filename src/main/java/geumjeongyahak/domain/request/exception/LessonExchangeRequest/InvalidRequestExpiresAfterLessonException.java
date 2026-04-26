package geumjeongyahak.domain.request.exception.LessonExchangeRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class InvalidRequestExpiresAfterLessonException extends BusinessException {

    public InvalidRequestExpiresAfterLessonException() {
        super(RequestErrorCode.INVALID_REQUEST_EXPIRES_AFTER_LESSON);
    }
}
