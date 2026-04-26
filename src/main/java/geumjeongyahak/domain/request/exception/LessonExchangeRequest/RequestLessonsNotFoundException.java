package geumjeongyahak.domain.request.exception.LessonExchangeRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class RequestLessonsNotFoundException extends BusinessException {

    public RequestLessonsNotFoundException() {
        super(RequestErrorCode.REQUEST_LESSONS_NOT_FOUND);
    }
}
