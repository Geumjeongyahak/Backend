package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;

public class InvalidDailyScheduleJournalStateException extends BusinessException {

    public InvalidDailyScheduleJournalStateException(Long dailyScheduleId, DailyScheduleStatus status) {
        super(
            DailyScheduleErrorCode.INVALID_DAILY_SCHEDULE_JOURNAL_STATE,
            "수업 일지를 저장할 수 없는 하루 일정 상태입니다. (dailyScheduleId: " + dailyScheduleId
                + ", status: " + status + ")"
        );
    }
}
