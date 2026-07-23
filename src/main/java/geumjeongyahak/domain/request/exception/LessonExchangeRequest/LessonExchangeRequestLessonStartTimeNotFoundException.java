package geumjeongyahak.domain.request.exception.LessonExchangeRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class LessonExchangeRequestLessonStartTimeNotFoundException extends BusinessException {

    public LessonExchangeRequestLessonStartTimeNotFoundException() {
        super(RequestErrorCode.LESSON_EXCHANGE_REQUEST_LESSON_START_TIME_NOT_FOUND);
    }
}
