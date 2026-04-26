package geumjeongyahak.domain.request.exception.LessonExchangeProposal;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class ProposalTimeOverlapWithRequestException extends BusinessException {

    public ProposalTimeOverlapWithRequestException() {
        super(RequestErrorCode.PROPOSAL_TIME_OVERLAPS_REQUEST);
    }
}
