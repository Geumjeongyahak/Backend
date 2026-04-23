package geumjeongyahak.domain.request.exception;

import geumjeongyahak.common.exception.BusinessException;

public class DuplicateActiveRequestException extends BusinessException {

    public DuplicateActiveRequestException() {
        super(RequestErrorCode.DUPLICATE_ACTIVE_REQUEST);
    }
}
