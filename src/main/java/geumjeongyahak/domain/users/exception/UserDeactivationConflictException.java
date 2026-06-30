package geumjeongyahak.domain.users.exception;

import geumjeongyahak.common.exception.BusinessException;

public class UserDeactivationConflictException extends BusinessException {

    private UserDeactivationConflictException(UserErrorCode errorCode) {
        super(errorCode);
    }

    public static UserDeactivationConflictException selfDeactivationBlocked() {
        return new UserDeactivationConflictException(UserErrorCode.CANNOT_DEACTIVATE_SELF);
    }

    public static UserDeactivationConflictException lastAdminDeactivationBlocked() {
        return new UserDeactivationConflictException(UserErrorCode.CANNOT_DEACTIVATE_LAST_ADMIN);
    }

    public static UserDeactivationConflictException activeWorkflowExists() {
        return new UserDeactivationConflictException(
            UserErrorCode.CANNOT_DEACTIVATE_USER_WITH_ACTIVE_WORKFLOW
        );
    }
}
