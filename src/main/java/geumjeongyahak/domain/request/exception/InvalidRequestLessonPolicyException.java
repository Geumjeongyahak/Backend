package geumjeongyahak.domain.request.exception;

import geumjeongyahak.common.exception.BusinessException;

public class InvalidRequestLessonPolicyException extends BusinessException {

    public InvalidRequestLessonPolicyException() {
        super(RequestErrorCode.INVALID_REQUEST_LESSON_POLICY);
    }
}
