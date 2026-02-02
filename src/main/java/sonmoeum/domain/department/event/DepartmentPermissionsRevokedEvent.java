package sonmoeum.domain.department.event;

import java.util.Collection;

import sonmoeum.domain.auth.enums.PermissionType;

public record DepartmentPermissionsRevokedEvent(
    Long departmentId,
    Collection<PermissionType> permissions
) {}
