package geumjeongyahak.domain.request.exception.LessonExchangeProposal;

import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class ProposalNotFoundException extends ResourceNotFoundException {
    public ProposalNotFoundException(Long proposalId) {
        super(RequestErrorCode.PROPOSAL_NOT_FOUND, proposalId);
    }
}
