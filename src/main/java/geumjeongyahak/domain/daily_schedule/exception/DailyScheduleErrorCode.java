package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DailyScheduleErrorCode implements ErrorCode {
    DAILY_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-11-001", "하루 일정을 찾을 수 없습니다."),
    DAILY_SCHEDULE_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH-11-001", "하루 일정에 접근할 권한이 없습니다."),
    LESSON_NOT_IN_DAILY_SCHEDULE(HttpStatus.BAD_REQUEST, "VAL-11-001", "하루 일정에 연결되지 않은 수업입니다."),
    INVALID_DAILY_SCHEDULE_JOURNAL_STATE(HttpStatus.BAD_REQUEST, "VAL-11-002", "수업 일지를 저장할 수 없는 하루 일정 상태입니다."),
    INVALID_DAILY_SCHEDULE_PERSONAL_INFO(HttpStatus.BAD_REQUEST, "VAL-11-003", "수업 일지 개인정보 입력값이 유효하지 않습니다."),
    STUDENT_NOT_IN_DAILY_SCHEDULE(HttpStatus.BAD_REQUEST, "VAL-11-004", "하루 일정에 연결되지 않은 학생입니다."),
    INVALID_DAILY_SCHEDULE_ATTENDANCE_STATE(HttpStatus.BAD_REQUEST, "VAL-11-005", "학생 출석을 처리할 수 없는 하루 일정 상태입니다."),
    DUPLICATE_DAILY_STUDENT_ATTENDANCE(HttpStatus.BAD_REQUEST, "VAL-11-006", "중복된 학생 출석 요청입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
