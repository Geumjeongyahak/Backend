package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;

public class DailyTeacherAttendanceRequiredException extends BusinessException {

    public DailyTeacherAttendanceRequiredException(Long dailyScheduleId) {
        super(
            DailyScheduleErrorCode.DAILY_TEACHER_ATTENDANCE_REQUIRED,
            "출근 처리 이후에만 퇴근 처리할 수 있습니다. (dailyScheduleId: " + dailyScheduleId + ")"
        );
    }
}
