package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;

public class DailyScheduleJournalAlreadyExistsException extends BusinessException {

    public DailyScheduleJournalAlreadyExistsException(Long dailyScheduleId) {
        super(
            DailyScheduleErrorCode.DAILY_SCHEDULE_JOURNAL_ALREADY_EXISTS,
            "이미 작성된 수업 일지가 있습니다. (dailyScheduleId: " + dailyScheduleId + ")"
        );
    }
}
