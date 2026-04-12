package sonmoeum.domain.request.exception;

import sonmoeum.common.exception.BusinessException;

public class RequestForbiddenException extends BusinessException {

    public RequestForbiddenException() {
        super(RequestErrorCode.REQUEST_FORBIDDEN);
    }
}
