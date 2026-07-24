package geumjeongyahak.domain.request.exception.AbsenceRequest;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.request.exception.RequestErrorCode;

public class AbsenceRequestLessonStartTimeNotFoundException extends BusinessException {

    public AbsenceRequestLessonStartTimeNotFoundException() {
        super(RequestErrorCode.ABSENCE_REQUEST_LESSON_START_TIME_NOT_FOUND);
    }
}
