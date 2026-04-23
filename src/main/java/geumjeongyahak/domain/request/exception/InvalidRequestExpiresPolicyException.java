package geumjeongyahak.domain.request.exception;

import geumjeongyahak.common.exception.BusinessException;

public class InvalidRequestExpiresPolicyException extends BusinessException {

    public InvalidRequestExpiresPolicyException() {
        super(RequestErrorCode.INVALID_REQUEST_EXPIRES_POLICY);
    }
}
