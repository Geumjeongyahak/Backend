package geumjeongyahak.domain.request.exception.LessonExchangeProposal;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class MultipleClassroomsInLessonExchangeProposalException extends BusinessException {

    public MultipleClassroomsInLessonExchangeProposalException() {
        super(RequestErrorCode.MULTIPLE_CLASSROOMS_IN_LESSON_EXCHANGE_PROPOSAL);
    }
}
