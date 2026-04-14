package geumjeongyahak.domain.request.exception;

import geumjeongyahak.common.exception.BusinessException;

public class RequestForbiddenException extends BusinessException {

    public RequestForbiddenException() {
        super(RequestErrorCode.REQUEST_FORBIDDEN);
    }
}
