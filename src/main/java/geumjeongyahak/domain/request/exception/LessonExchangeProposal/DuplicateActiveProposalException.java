package geumjeongyahak.domain.request.exception.LessonExchangeProposal;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class DuplicateActiveProposalException extends BusinessException {

    public DuplicateActiveProposalException() {
        super(RequestErrorCode.DUPLICATE_ACTIVE_PROPOSAL);
    }
}
