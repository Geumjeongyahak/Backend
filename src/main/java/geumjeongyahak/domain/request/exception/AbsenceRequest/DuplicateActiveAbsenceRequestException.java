package geumjeongyahak.domain.request.exception.AbsenceRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class DuplicateActiveAbsenceRequestException extends BusinessException {

    public DuplicateActiveAbsenceRequestException() {
        super(RequestErrorCode.DUPLICATE_ACTIVE_ABSENCE_REQUEST);
    }
}
