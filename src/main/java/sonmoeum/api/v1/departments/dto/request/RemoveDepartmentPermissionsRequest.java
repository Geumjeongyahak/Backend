package sonmoeum.api.v1.departments.dto.request;

import java.util.List;

import sonmoeum.common.validation.annotation.ValidPermissions;

public record RemoveDepartmentPermissionsRequest(
    @ValidPermissions
    List<String> permissions
) {
} 
