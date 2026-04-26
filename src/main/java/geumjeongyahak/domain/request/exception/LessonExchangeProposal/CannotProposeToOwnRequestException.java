package geumjeongyahak.domain.request.exception.LessonExchangeProposal;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class CannotProposeToOwnRequestException extends BusinessException {

    public CannotProposeToOwnRequestException() {
        super(RequestErrorCode.CANNOT_PROPOSE_TO_OWN_REQUEST);
    }
}
