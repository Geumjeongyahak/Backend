package sonmoeum.domain.department.exception;

import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.ErrorCode;
import sonmoeum.domain.auth.enums.RoleType;

public class DeleteDepartmentWithRoleException extends BusinessException {
    private static final ErrorCode CODE = ErrorCode.CANNOT_DELETE_ROLE_IN_USE;

    public DeleteDepartmentWithRoleException(Long deptId, RoleType roleType) {
        super(CODE,
            String.format("해당 부서(ID: %d)는 현재 역할(%s)이 할당되어 있어 삭제할 수 없습니다.", deptId, roleType.name()));
    }
}
