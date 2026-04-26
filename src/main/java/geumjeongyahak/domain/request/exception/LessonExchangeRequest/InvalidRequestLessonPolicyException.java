package geumjeongyahak.domain.request.exception.LessonExchangeRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class InvalidRequestLessonPolicyException extends BusinessException {

    public InvalidRequestLessonPolicyException() {
        super(RequestErrorCode.INVALID_REQUEST_LESSON_POLICY);
    }
}
