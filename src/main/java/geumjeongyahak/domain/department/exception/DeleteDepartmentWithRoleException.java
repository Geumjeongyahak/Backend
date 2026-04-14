package geumjeongyahak.domain.department.exception;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.ErrorCode;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.users.exception.UserErrorCode;

public class DeleteDepartmentWithRoleException extends BusinessException {
    private static final ErrorCode CODE = UserErrorCode.CANNOT_DELETE_ROLE_IN_USE;

    public DeleteDepartmentWithRoleException(Long deptId, RoleType roleType) {
        super(CODE,
            String.format("해당 부서(ID: %d)는 현재 역할(%s)이 할당되어 있어 삭제할 수 없습니다.", deptId, roleType.name()));
    }
}
