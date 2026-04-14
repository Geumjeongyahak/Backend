package geumjeongyahak.domain.auth.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.auth.enums.RoleType;

@Schema(description = "역할(권한) 정보")
public record RoleResponse(
        @Schema(description = "역할 이름", example = "ADMIN")
        String name,

        @Schema(description = "역할 레벨", example = "100")
        String level,

        @Schema(description = "역할 코드", example = "1")
        Long code
) {
    public static RoleResponse from(RoleType roleType) {
        return new RoleResponse(
                roleType.name(),
                roleType.getLevel().name(),
                roleType.getId()
        );
    }
}
