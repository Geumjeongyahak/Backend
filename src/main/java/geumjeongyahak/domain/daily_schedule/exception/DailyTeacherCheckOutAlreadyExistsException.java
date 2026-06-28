package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;

public class DailyTeacherCheckOutAlreadyExistsException extends BusinessException {

    public DailyTeacherCheckOutAlreadyExistsException(Long dailyScheduleId) {
        super(
            DailyScheduleErrorCode.DAILY_TEACHER_CHECK_OUT_ALREADY_EXISTS,
            "이미 퇴근 처리된 교사 출석입니다. (dailyScheduleId: " + dailyScheduleId + ")"
        );
    }
}
