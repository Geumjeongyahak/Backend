package sonmoeum.domain.department.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import sonmoeum.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum DepartmentErrorCode implements ErrorCode {
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RES-02-001", "부서를 찾을 수 없습니다."),
    USER_NOT_IN_DEPARTMENT(HttpStatus.NOT_FOUND, "BIZ-02-002", "해당 부서에 소속되지 않은 사용자입니다."),
    CANNOT_DELETE_DEPARTMENT_IN_USE(HttpStatus.BAD_REQUEST, "BIZ-02-001", "해당 부서가 사용자에게 할당되어 있어 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
