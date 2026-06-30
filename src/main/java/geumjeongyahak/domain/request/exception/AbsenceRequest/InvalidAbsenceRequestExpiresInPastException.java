package geumjeongyahak.domain.request.exception.AbsenceRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class InvalidAbsenceRequestExpiresInPastException extends BusinessException {

    public InvalidAbsenceRequestExpiresInPastException() {
        super(RequestErrorCode.INVALID_REQUEST_EXPIRES_IN_PAST);
    }
}
