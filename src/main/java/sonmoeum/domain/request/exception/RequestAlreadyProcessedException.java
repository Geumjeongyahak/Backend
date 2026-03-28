package sonmoeum.domain.request.exception;

import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.ErrorCode;

public class RequestAlreadyProcessedException extends BusinessException {

    public RequestAlreadyProcessedException() {
        super(ErrorCode.REQUEST_ALREADY_PROCESSED);
    }
}
