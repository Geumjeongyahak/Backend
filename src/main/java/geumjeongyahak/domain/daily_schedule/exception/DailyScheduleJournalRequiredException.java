package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;

public class DailyScheduleJournalRequiredException extends BusinessException {

    public DailyScheduleJournalRequiredException(Long dailyScheduleId) {
        super(
            DailyScheduleErrorCode.DAILY_SCHEDULE_JOURNAL_REQUIRED,
            "수업 일지 작성 이후에만 퇴근 처리할 수 있습니다. (dailyScheduleId: " + dailyScheduleId + ")"
        );
    }
}
