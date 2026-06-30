package geumjeongyahak.domain.request.exception.LessonExchangeProposal;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class RequestNotAcceptableException extends BusinessException {

    public RequestNotAcceptableException() {
        super(RequestErrorCode.REQUEST_NOT_ACCEPTABLE);
    }
}
