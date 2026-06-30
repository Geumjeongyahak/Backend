package geumjeongyahak.domain.request.exception;

import geumjeongyahak.common.exception.BusinessException;

public class RequestAlreadyProcessedException extends BusinessException {

    public RequestAlreadyProcessedException() {
        super(RequestErrorCode.REQUEST_ALREADY_PROCESSED);
    }
}
