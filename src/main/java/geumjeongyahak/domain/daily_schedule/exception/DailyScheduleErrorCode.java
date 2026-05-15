package geumjeongyahak.domain.daily_schedule.exception;

import geumjeongyahak.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DailyScheduleErrorCode implements ErrorCode {
    DAILY_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-11-001", "하루 일정을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
