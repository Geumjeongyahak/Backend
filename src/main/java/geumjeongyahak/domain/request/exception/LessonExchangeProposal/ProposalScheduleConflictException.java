package geumjeongyahak.domain.request.exception.LessonExchangeProposal;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class ProposalScheduleConflictException extends BusinessException {

    public ProposalScheduleConflictException() {
        super(RequestErrorCode.PROPOSAL_SCHEDULE_CONFLICT);
    }
}
