package sonmoeum.api.v1.users.dto.request;

import java.util.List;

import sonmoeum.common.validation.annotation.ValidPermissions;

public record AddUserPermissionsRequest(
    @ValidPermissions
    List<String> permissions
) {

}
