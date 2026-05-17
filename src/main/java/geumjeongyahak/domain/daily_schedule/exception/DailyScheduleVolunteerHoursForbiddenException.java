package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;

public class DailyScheduleVolunteerHoursForbiddenException extends BusinessException {

    public DailyScheduleVolunteerHoursForbiddenException(Long requesterId, Long teacherId) {
        super(
            DailyScheduleErrorCode.DAILY_SCHEDULE_FORBIDDEN,
            "다른 교사의 봉사 시간을 조회할 권한이 없습니다. (requesterId: " + requesterId + ", teacherId: " + teacherId + ")"
        );
    }
}
