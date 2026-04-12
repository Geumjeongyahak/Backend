package sonmoeum.domain.request.exception;

import sonmoeum.common.exception.BusinessException;

public class RequestAlreadyProcessedException extends BusinessException {

    public RequestAlreadyProcessedException() {
        super(RequestErrorCode.REQUEST_ALREADY_PROCESSED);
    }
}
