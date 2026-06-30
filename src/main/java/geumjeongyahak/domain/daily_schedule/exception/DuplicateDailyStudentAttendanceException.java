package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.BusinessException;

public class DuplicateDailyStudentAttendanceException extends BusinessException {

    public DuplicateDailyStudentAttendanceException(Long dailyScheduleId, Long studentId) {
        super(
            DailyScheduleErrorCode.DUPLICATE_DAILY_STUDENT_ATTENDANCE,
            "중복된 학생 출석 요청입니다. (dailyScheduleId: " + dailyScheduleId + ", studentId: " + studentId + ")"
        );
    }
}
