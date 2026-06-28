package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;
import java.time.LocalDateTime;

public class InvalidDailyTeacherCheckOutTimeException extends BusinessException {

    public InvalidDailyTeacherCheckOutTimeException(
        Long dailyScheduleId,
        LocalDateTime attendedAt,
        LocalDateTime checkedOutAt
    ) {
        super(
            DailyScheduleErrorCode.INVALID_DAILY_TEACHER_CHECK_OUT_TIME,
            "퇴근 시간이 출근 시간보다 빠를 수 없습니다. (dailyScheduleId: " + dailyScheduleId
                + ", attendedAt: " + attendedAt + ", checkedOutAt: " + checkedOutAt + ")"
        );
    }
}
