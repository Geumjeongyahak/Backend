package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;

public class StudentNotInDailyScheduleException extends BusinessException {

    public StudentNotInDailyScheduleException(Long dailyScheduleId, Long studentId) {
        super(
            DailyScheduleErrorCode.STUDENT_NOT_IN_DAILY_SCHEDULE,
            "하루 일정에 연결되지 않은 학생입니다. (dailyScheduleId: " + dailyScheduleId + ", studentId: " + studentId + ")"
        );
    }
}
