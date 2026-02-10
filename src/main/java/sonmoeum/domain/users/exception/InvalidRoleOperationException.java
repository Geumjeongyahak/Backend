package sonmoeum.domain.users.exception;

import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.ErrorCode;

/**
 * 역할(Role) 조작 관련 예외
 * - 기본 역할 직접 부여/제거 시도
 * - 이미 부여된 역할 중복 부여
 * - 부여되지 않은 역할 제거 시도
 */
public class InvalidRoleOperationException extends BusinessException {

    private InvalidRoleOperationException(ErrorCode errorCode, String message) {
        super(message, errorCode.getStatus(), errorCode.getCode());
    }

    /**
     * 기본 역할을 직접 부여하려고 시도한 경우
     */
    public static InvalidRoleOperationException cannotAssignBaseRole(String roleType) {
        return new InvalidRoleOperationException(
                ErrorCode.CANNOT_ASSIGN_BASE_ROLE,
                ErrorCode.CANNOT_ASSIGN_BASE_ROLE.getMessage() + " (RoleType: " + roleType + ")"
        );
    }

    /**
     * 기본 역할을 직접 제거하려고 시도한 경우
     */
    public static InvalidRoleOperationException cannotRemoveBaseRole(String roleType) {
        return new InvalidRoleOperationException(
                ErrorCode.CANNOT_REMOVE_BASE_ROLE,
                ErrorCode.CANNOT_REMOVE_BASE_ROLE.getMessage() + " (RoleType: " + roleType + ")"
        );
    }

    /**
     * 이미 부여된 역할을 다시 부여하려고 시도한 경우
     */
    public static InvalidRoleOperationException roleAlreadyAssigned(Long userId, String roleType) {
        return new InvalidRoleOperationException(
                ErrorCode.ROLE_ALREADY_ASSIGNED,
                ErrorCode.ROLE_ALREADY_ASSIGNED.getMessage() + " (UserID: " + userId + ", RoleType: " + roleType + ")"
        );
    }

    /**
     * 부여되지 않은 역할을 제거하려고 시도한 경우
     */
    public static InvalidRoleOperationException roleNotAssigned(Long userId, String roleType) {
        return new InvalidRoleOperationException(
                ErrorCode.ROLE_NOT_ASSIGNED,
                ErrorCode.ROLE_NOT_ASSIGNED.getMessage() + " (UserID: " + userId + ", RoleType: " + roleType + ")"
        );
    }
}
