package geumjeongyahak.domain.department.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidPermissionCode;
import geumjeongyahak.domain.department.enums.DepartmentRoleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부서 직책별 권한 요청 DTO")
public record DepartmentPermissionRequest(
    @Schema(description = "부서 직책 유형. MEMBER는 일반 부서원 권한, MANAGER는 부서장 추가 권한입니다. 생략하면 MEMBER로 처리됩니다.", example = "MANAGER", nullable = true)
    DepartmentRoleType roleType,

    @Schema(description = "권한 코드. PermissionRegistry에서 허용한 authority 문자열이어야 합니다.", example = "channel:write:12")
    @ValidPermissionCode
    String permissionCode
) {
    public DepartmentRoleType roleTypeOrDefault() {
        return roleType != null ? roleType : DepartmentRoleType.MEMBER;
    }
}
