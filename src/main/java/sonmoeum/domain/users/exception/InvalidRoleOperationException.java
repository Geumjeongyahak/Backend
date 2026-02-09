package sonmoeum.domain.users.exception;

import org.springframework.http.HttpStatus;
import sonmoeum.common.exception.BusinessException;

public class InvalidRoleOperationException extends BusinessException {
    private static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;
    private static final String CODE = "INVALID_ROLE_OPERATION";

    public InvalidRoleOperationException(String message) {
        super(message, STATUS, CODE);
    }

    public static InvalidRoleOperationException cannotAssignBaseRole(String roleType) {
        return new InvalidRoleOperationException("기본 역할은 직접 부여할 수 없습니다. RoleType: " + roleType);
    }

    public static InvalidRoleOperationException cannotRemoveBaseRole(String roleType) {
        return new InvalidRoleOperationException("기본 역할은 직접 제거할 수 없습니다. RoleType: " + roleType);
    }

    public static InvalidRoleOperationException roleAlreadyAssigned(Long userId, String roleType) {
        return new InvalidRoleOperationException("사용자에게 이미 해당 역할이 부여되어 있습니다. UserID: " + userId + ", RoleType: " + roleType);
    }

    public static InvalidRoleOperationException roleNotAssigned(Long userId, String roleType) {
        return new InvalidRoleOperationException("사용자에게 해당 역할이 부여되어 있지 않습니다. UserID: " + userId + ", RoleType: " + roleType);
    }
}
