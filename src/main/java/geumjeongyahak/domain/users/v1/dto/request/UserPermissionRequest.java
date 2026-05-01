package geumjeongyahak.domain.users.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidPermissionCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 권한 요청 DTO")
public record UserPermissionRequest(
    @Schema(description = "권한 코드", example = "channel:write:*")
    @ValidPermissionCode
    String permissionCode
) {
}
