package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;

public class DailyScheduleForbiddenException extends BusinessException {

    public DailyScheduleForbiddenException(Long dailyScheduleId, Long userId) {
        super(
            DailyScheduleErrorCode.DAILY_SCHEDULE_FORBIDDEN,
            "하루 일정에 접근할 권한이 없습니다. (dailyScheduleId: " + dailyScheduleId + ", userId: " + userId + ")"
        );
    }
}
