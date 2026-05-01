package geumjeongyahak.domain.request.exception.LessonExchangeProposal;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class RequestNotProposableException extends BusinessException {

    public RequestNotProposableException() {
        super(RequestErrorCode.REQUEST_NOT_PROPOSABLE);
    }
}
