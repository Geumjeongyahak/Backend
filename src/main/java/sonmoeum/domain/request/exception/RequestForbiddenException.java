package sonmoeum.domain.request.exception;

import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.ErrorCode;

public class RequestForbiddenException extends BusinessException {

    public RequestForbiddenException() {
        super(ErrorCode.REQUEST_FORBIDDEN);
    }
}
