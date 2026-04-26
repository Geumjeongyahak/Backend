package geumjeongyahak.domain.request.exception.LessonExchangeRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class InvalidRequestExpiresInPastException extends BusinessException {

    public InvalidRequestExpiresInPastException() {
        super(RequestErrorCode.INVALID_REQUEST_EXPIRES_IN_PAST);
    }
}
