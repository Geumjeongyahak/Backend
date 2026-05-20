package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;

public class InvalidDailyScheduleJournalLessonsException extends BusinessException {

    public InvalidDailyScheduleJournalLessonsException(Long dailyScheduleId) {
        super(
            DailyScheduleErrorCode.INVALID_DAILY_SCHEDULE_JOURNAL_LESSONS,
            "하루 일정에 연결된 모든 교시의 수업 일지를 입력해야 합니다. (dailyScheduleId: " + dailyScheduleId + ")"
        );
    }
}
