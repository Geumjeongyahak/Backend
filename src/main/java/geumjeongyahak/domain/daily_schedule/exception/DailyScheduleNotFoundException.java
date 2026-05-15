package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.ResourceNotFoundException;

public class DailyScheduleNotFoundException extends ResourceNotFoundException {

    public DailyScheduleNotFoundException(Long dailyScheduleId) {
        super(DailyScheduleErrorCode.DAILY_SCHEDULE_NOT_FOUND, dailyScheduleId);
    }
}
