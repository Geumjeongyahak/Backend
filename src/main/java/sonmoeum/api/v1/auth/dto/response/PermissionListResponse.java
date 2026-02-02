package sonmoeum.api.v1.auth.dto.response;

import java.util.List;

public record PermissionListResponse(
    List<String> permissions
) {
    
}
