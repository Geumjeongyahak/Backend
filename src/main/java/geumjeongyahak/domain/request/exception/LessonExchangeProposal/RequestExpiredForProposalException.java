package geumjeongyahak.domain.request.exception.LessonExchangeProposal;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class RequestExpiredForProposalException extends BusinessException {

    public RequestExpiredForProposalException() {
        super(RequestErrorCode.REQUEST_EXPIRED_FOR_PROPOSAL);
    }
}
