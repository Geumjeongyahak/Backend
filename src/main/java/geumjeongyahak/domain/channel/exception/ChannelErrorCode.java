package geumjeongyahak.domain.channel.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import geumjeongyahak.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ChannelErrorCode implements ErrorCode {
    CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-08-001", "채널을 찾을 수 없습니다."),
    DUPLICATE_CHANNEL(HttpStatus.CONFLICT, "BIZ-08-001", "이미 존재하는 채널입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
