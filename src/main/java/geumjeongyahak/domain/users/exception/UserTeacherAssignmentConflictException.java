package geumjeongyahak.domain.users.exception;

import geumjeongyahak.common.exception.BusinessException;

public class UserTeacherAssignmentConflictException extends BusinessException {

    private UserTeacherAssignmentConflictException(UserErrorCode errorCode) {
        super(errorCode);
    }

    public static UserTeacherAssignmentConflictException roleChangeBlocked() {
        return new UserTeacherAssignmentConflictException(
            UserErrorCode.CANNOT_CHANGE_ROLE_WITH_ACTIVE_ASSIGNMENT
        );
    }

    public static UserTeacherAssignmentConflictException deletionBlocked() {
        return new UserTeacherAssignmentConflictException(
            UserErrorCode.CANNOT_DELETE_USER_WITH_ACTIVE_ASSIGNMENT
        );
    }
}
