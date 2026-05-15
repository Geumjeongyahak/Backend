package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;

public class LessonNotInDailyScheduleException extends BusinessException {

    public LessonNotInDailyScheduleException(Long dailyScheduleId, Long lessonId) {
        super(
            DailyScheduleErrorCode.LESSON_NOT_IN_DAILY_SCHEDULE,
            "하루 일정에 연결되지 않은 수업입니다. (dailyScheduleId: " + dailyScheduleId + ", lessonId: " + lessonId + ")"
        );
    }
}
