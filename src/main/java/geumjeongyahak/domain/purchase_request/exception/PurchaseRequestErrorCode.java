package geumjeongyahak.domain.purchase_request.exception;

import geumjeongyahak.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PurchaseRequestErrorCode implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "PR-001", "구입 요청을 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "PR-002", "해당 구입 요청에 대한 권한이 없습니다."),
    ALREADY_PROCESSED(HttpStatus.CONFLICT, "PR-003", "이미 처리된 구입 요청입니다."),
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "PR-004", "구입 요청 항목을 찾을 수 없습니다."),
    PURCHASE_DEADLINE_EXCEEDED(HttpStatus.CONFLICT, "PR-005", "구매 기한(승인 후 7일)이 초과되었습니다."),
    INVALID_STATUS(HttpStatus.CONFLICT, "PR-006", "현재 상태에서는 처리할 수 없는 요청입니다."),
    INVALID_PAYMENT_METHOD(HttpStatus.BAD_REQUEST, "PR-007", "결제 방식과 거래처 정보가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
