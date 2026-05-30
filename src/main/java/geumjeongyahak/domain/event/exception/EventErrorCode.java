package geumjeongyahak.domain.event.exception;

import org.springframework.http.HttpStatus;

import geumjeongyahak.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventErrorCode implements ErrorCode {
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-13-001", "행사를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
