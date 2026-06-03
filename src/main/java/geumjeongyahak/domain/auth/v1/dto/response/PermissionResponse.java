package geumjeongyahak.domain.auth.v1.dto.response;

import geumjeongyahak.domain.auth.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역할/권한 정보")
public record PermissionResponse(
    @Schema(description = "이름", example = "ADMIN")
    String name,

    @Schema(description = "코드", example = "department:write:*")
    String code,

    @Schema(description = "권한 출처. 사용자 예외 권한은 MANUAL, 부서 직책 권한은 MEMBER/MANAGER로 내려갑니다.", example = "MANUAL", nullable = true)
    String source
) {
    public static final String SOURCE_MANUAL = "MANUAL";

    public PermissionResponse(String name, String code) {
        this(name, code, null);
    }

    public static PermissionResponse from(RoleType roleType) {
        return new PermissionResponse(roleType.name(), null);
    }
}
