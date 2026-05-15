package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;

public class InvalidDailyScheduleAttendanceStateException extends BusinessException {

    public InvalidDailyScheduleAttendanceStateException(Long dailyScheduleId, DailyScheduleStatus status) {
        super(
            DailyScheduleErrorCode.INVALID_DAILY_SCHEDULE_ATTENDANCE_STATE,
            "출석을 처리할 수 없는 하루 일정 상태입니다. (dailyScheduleId: " + dailyScheduleId
                + ", status: " + status + ")"
        );
    }
}
