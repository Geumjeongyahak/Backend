package geumjeongyahak.domain.auth.v1.dto.response;

import geumjeongyahak.domain.auth.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역할/권한 정보")
public record PermissionResponse(
    @Schema(description = "이름", example = "ADMIN")
    String name,

    @Schema(description = "코드", example = "department:write:*")
    String code
) {
    public static PermissionResponse from(RoleType roleType) {
        return new PermissionResponse(roleType.name(), null);
    }
}
