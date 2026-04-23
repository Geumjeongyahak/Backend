package geumjeongyahak.domain.request.exception;

import geumjeongyahak.common.exception.BusinessException;

public class InvalidRequestExpiresInPastException extends BusinessException {

    public InvalidRequestExpiresInPastException() {
        super(RequestErrorCode.INVALID_REQUEST_EXPIRES_IN_PAST);
    }
}
