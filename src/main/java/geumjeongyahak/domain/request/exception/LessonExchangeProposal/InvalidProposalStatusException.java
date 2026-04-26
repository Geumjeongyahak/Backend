package geumjeongyahak.domain.request.exception.LessonExchangeProposal;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class InvalidProposalStatusException extends BusinessException {

    public InvalidProposalStatusException() {
        super(RequestErrorCode.INVALID_PROPOSAL_STATUS);
    }
}
