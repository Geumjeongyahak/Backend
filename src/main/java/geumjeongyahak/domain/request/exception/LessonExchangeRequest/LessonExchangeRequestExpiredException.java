package geumjeongyahak.domain.request.exception.LessonExchangeRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class LessonExchangeRequestExpiredException extends BusinessException {

    public LessonExchangeRequestExpiredException() {
        super(RequestErrorCode.LESSON_EXCHANGE_REQUEST_EXPIRED);
    }
}
