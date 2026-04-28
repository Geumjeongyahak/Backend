package geumjeongyahak.domain.department.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidPermissionCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부서 권한 요청 DTO")
public record DepartmentPermissionRequest(
    @Schema(description = "권한 코드", example = "classroom:write:12")
    @ValidPermissionCode
    String permissionCode
) {
}
