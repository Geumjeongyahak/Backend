package geumjeongyahak.domain.users.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.common.validation.annotation.ValidRole;
import geumjeongyahak.domain.auth.enums.RoleLevel;

@Schema(description = "사용자 하위 역할 추가 요청 DTO")
public record AddSubRoleRequest(
        @Schema(description = "추가할 하위 역할", example = "TEACHER")
        @ValidRole(levels = { RoleLevel.ADDITIONAL, RoleLevel.DEPARTMENT }, message = "유효하지 않은 하위 역할입니다.")
        String subRole
) {

}
