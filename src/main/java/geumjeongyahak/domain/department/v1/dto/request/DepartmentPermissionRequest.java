package geumjeongyahak.domain.department.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidPermissionCode;
import geumjeongyahak.domain.department.enums.DepartmentRoleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부서 권한 요청 DTO")
public record DepartmentPermissionRequest(
    @Schema(description = "부서 직책 유형. 생략하면 MEMBER로 처리됩니다.", example = "MANAGER")
    DepartmentRoleType roleType,

    @Schema(description = "권한 코드", example = "channel:write:12")
    @ValidPermissionCode
    String permissionCode
) {
    public DepartmentRoleType roleTypeOrDefault() {
        return roleType != null ? roleType : DepartmentRoleType.MEMBER;
    }
}
