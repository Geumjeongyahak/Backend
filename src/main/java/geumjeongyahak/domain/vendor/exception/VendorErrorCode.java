package geumjeongyahak.domain.vendor.exception;

import geumjeongyahak.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VendorErrorCode implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "VEN-001", "거래처를 찾을 수 없습니다."),
    INACTIVE(HttpStatus.CONFLICT, "VEN-002", "비활성 거래처는 사용할 수 없습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "VEN-003", "거래처 잔액이 부족합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
