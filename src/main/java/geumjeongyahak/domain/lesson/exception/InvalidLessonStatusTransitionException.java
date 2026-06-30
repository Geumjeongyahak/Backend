package geumjeongyahak.domain.lesson.exception;

import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.domain.lesson.enums.LessonStatus;

public class InvalidLessonStatusTransitionException extends BadRequestException {

    public InvalidLessonStatusTransitionException(LessonStatus currentStatus, LessonStatus nextStatus) {
        super(
            LessonErrorCode.INVALID_LESSON_STATUS_TRANSITION,
            "수업 상태는 SCHEDULED에서 COMPLETED 또는 CANCELED로만 변경할 수 있습니다. "
                + "(currentStatus=" + currentStatus + ", nextStatus=" + nextStatus + ")"
        );
    }
}
