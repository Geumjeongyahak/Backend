package geumjeongyahak.domain.request.exception.LessonExchangeRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class InvalidRequestExpiresPolicyException extends BusinessException {

    public InvalidRequestExpiresPolicyException() {
        super(RequestErrorCode.INVALID_REQUEST_EXPIRES_POLICY);
    }
}
