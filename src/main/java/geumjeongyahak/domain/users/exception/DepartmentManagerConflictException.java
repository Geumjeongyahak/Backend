package geumjeongyahak.domain.users.exception;

import geumjeongyahak.common.exception.BusinessException;

public class DepartmentManagerConflictException extends BusinessException {

    private DepartmentManagerConflictException(UserErrorCode errorCode, Long departmentId) {
        super(errorCode, errorCode.getMessage() + " departmentId=" + departmentId);
    }

    public static DepartmentManagerConflictException alreadyExists(Long departmentId) {
        return new DepartmentManagerConflictException(
            UserErrorCode.ACTIVE_DEPARTMENT_MANAGER_ALREADY_EXISTS,
            departmentId
        );
    }

    public static DepartmentManagerConflictException multipleManagers(Long departmentId) {
        return new DepartmentManagerConflictException(
            UserErrorCode.MULTIPLE_ACTIVE_DEPARTMENT_MANAGERS,
            departmentId
        );
    }
}
