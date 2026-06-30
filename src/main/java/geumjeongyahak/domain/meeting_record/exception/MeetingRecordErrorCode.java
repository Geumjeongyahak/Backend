package geumjeongyahak.domain.meeting_record.exception;

import geumjeongyahak.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MeetingRecordErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "MR-001", "교학 회의록을 찾을 수 없습니다."),
    ABSENCE_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "MR-002", "불참 사유서를 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "MR-003", "교학 회의록에 대한 권한이 없습니다."),
    STAFF_ONLY(HttpStatus.FORBIDDEN, "MR-004", "스태프만 이용할 수 있습니다."),
    INVALID_STATUS(HttpStatus.CONFLICT, "MR-005", "현재 상태에서는 처리할 수 없습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "MR-006", "입력값이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
