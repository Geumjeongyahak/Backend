package geumjeongyahak.domain.request.exception.LessonExchangeRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class LessonExchangeRequestLessonAlreadyStartedException extends BusinessException {

    public LessonExchangeRequestLessonAlreadyStartedException() {
        super(RequestErrorCode.LESSON_EXCHANGE_REQUEST_LESSON_ALREADY_STARTED);
    }
}
