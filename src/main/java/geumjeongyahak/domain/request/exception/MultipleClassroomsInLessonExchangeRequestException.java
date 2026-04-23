package geumjeongyahak.domain.request.exception;

import geumjeongyahak.common.exception.BusinessException;

public class MultipleClassroomsInLessonExchangeRequestException extends BusinessException {

    public MultipleClassroomsInLessonExchangeRequestException() {
        super(RequestErrorCode.MULTIPLE_CLASSROOMS_IN_LESSON_EXCHANGE_REQUEST);
    }
}
