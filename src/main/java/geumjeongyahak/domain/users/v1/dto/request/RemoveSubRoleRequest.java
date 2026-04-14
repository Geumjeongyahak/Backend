package geumjeongyahak.domain.users.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.common.validation.annotation.ValidRole;
import geumjeongyahak.domain.auth.enums.RoleLevel;

@Schema(description = "사용자 하위 역할 제거 요청 DTO")
public record RemoveSubRoleRequest (
    @Schema(description = "제거할 하위 역할", example = "TEACHER")
    @ValidRole(levels = { RoleLevel.DEPARTMENT, RoleLevel.ADDITIONAL }, message = "유효하지 않은 하위 역할입니다.")
    String subRole
) {
}
