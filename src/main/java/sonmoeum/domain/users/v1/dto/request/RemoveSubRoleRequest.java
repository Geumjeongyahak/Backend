package sonmoeum.domain.users.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import sonmoeum.common.validation.annotation.ValidRole;

public record RemoveSubRoleRequest (
    @Schema(description = "제거할 하위 역할", example = "TEACHER")
    @ValidRole(levels = { 1, 2 }, message = "유효하지 않은 하위 역할입니다.")
    String subRole
) {
}
