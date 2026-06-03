package geumjeongyahak.domain.auth.v1.dto.response;

import geumjeongyahak.domain.auth.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역할/권한 정보. 사용자 상세에서는 직접 권한과 부서 직책 권한의 출처를 함께 표현합니다.")
public record PermissionResponse(
    @Schema(description = "표시 이름. 별도 표시 이름이 없으면 권한 코드와 같은 값이 내려갈 수 있습니다.", example = "department:write:*")
    String name,

    @Schema(description = "권한 코드. 역할 응답에서는 null일 수 있습니다.", example = "department:write:*", nullable = true)
    String code,

    @Schema(description = "권한 출처. 사용자 직접 권한은 MANUAL, 부서 일반 부서원 권한은 MEMBER, 부서장 추가 권한은 MANAGER로 내려갑니다. 출처 구분이 없는 응답에서는 null일 수 있습니다.", example = "MANUAL", nullable = true)
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
