package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;

public class DailyScheduleJournalSheetLinkNotConfiguredException extends BusinessException {

    public DailyScheduleJournalSheetLinkNotConfiguredException() {
        super(DailyScheduleErrorCode.DAILY_SCHEDULE_JOURNAL_SHEET_LINK_NOT_CONFIGURED);
    }
}
