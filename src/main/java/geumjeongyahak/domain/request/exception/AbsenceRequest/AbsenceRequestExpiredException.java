package geumjeongyahak.domain.request.exception.AbsenceRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class AbsenceRequestExpiredException extends BusinessException {

    public AbsenceRequestExpiredException() {
        super(RequestErrorCode.ABSENCE_REQUEST_EXPIRED);
    }
}
