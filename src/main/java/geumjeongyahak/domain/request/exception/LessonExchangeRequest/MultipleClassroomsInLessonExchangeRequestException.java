package geumjeongyahak.domain.request.exception.LessonExchangeRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class MultipleClassroomsInLessonExchangeRequestException extends BusinessException {

    public MultipleClassroomsInLessonExchangeRequestException() {
        super(RequestErrorCode.MULTIPLE_CLASSROOMS_IN_LESSON_EXCHANGE_REQUEST);
    }
}
