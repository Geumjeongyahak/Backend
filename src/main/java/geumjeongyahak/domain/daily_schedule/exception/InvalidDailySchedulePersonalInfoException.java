package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;

public class InvalidDailySchedulePersonalInfoException extends BusinessException {

    public InvalidDailySchedulePersonalInfoException(Long dailyScheduleId) {
        super(
            DailyScheduleErrorCode.INVALID_DAILY_SCHEDULE_PERSONAL_INFO,
            "개인정보 활용 동의 여부와 주민번호 앞자리 입력값이 일치하지 않습니다. (dailyScheduleId: " + dailyScheduleId + ")"
        );
    }
}
