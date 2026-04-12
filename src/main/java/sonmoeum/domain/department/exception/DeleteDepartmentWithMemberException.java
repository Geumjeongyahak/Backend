package sonmoeum.domain.department.exception;

import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.ErrorCode;

public class DeleteDepartmentWithMemberException extends BusinessException {
    private static final ErrorCode CODE = DepartmentErrorCode.CANNOT_DELETE_DEPARTMENT_IN_USE;

    public DeleteDepartmentWithMemberException(Long deptId) {
        super(CODE, String.format("해당 부서(ID: %d)는 현재 구성원이 할당되어 있어 삭제할 수 없습니다.", deptId));
    }

}
