package geumjeongyahak.domain.request.exception.AbsenceRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class AbsenceRequestLessonAlreadyStartedException extends BusinessException {

    public AbsenceRequestLessonAlreadyStartedException() {
        super(RequestErrorCode.ABSENCE_REQUEST_LESSON_ALREADY_STARTED);
    }
}
