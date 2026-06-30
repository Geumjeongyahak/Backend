package geumjeongyahak.domain.request.exception.LessonExchangeProposal;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class ProposalLessonsNotFoundException extends BusinessException {

    public ProposalLessonsNotFoundException() {
        super(RequestErrorCode.PROPOSAL_LESSONS_NOT_FOUND);
    }
}
