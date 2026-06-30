package geumjeongyahak.domain.auth.v1.dto.response;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.base.model.PermissionCode;
import geumjeongyahak.domain.base.model.PermissionRegistry;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역할/권한 정보. 사용자 상세에서는 권한 메타데이터와 권한 출처를 함께 표현합니다.")
public record PermissionResponse(
    @Schema(description = "권한 식별자. 역할 권한이면 null입니다.", example = "1")
    Long id,

    @Schema(description = "이름", example = "ADMIN")
    String name,

    @Schema(description = "코드", example = "department:write:*")
    String code,

    @Schema(description = "권한 코드", example = "channel:write:1")
    String permissionCode,

    @Schema(description = "리소스 코드", example = "channel")
    String resourceCode,

    @Schema(description = "리소스 이름", example = "채널")
    String resourceLabel,

    @Schema(description = "액션 코드", example = "write")
    String actionCode,

    @Schema(description = "액션 이름", example = "작성")
    String actionLabel,

    @Schema(description = "권한 범위", example = "target", allowableValues = {"global", "target"})
    String scope,

    @Schema(description = "대상 식별자. 전체 권한이면 null입니다.", example = "1")
    Long targetId,

    @Schema(description = "대상 이름. 전체 권한이면 '전체', 개별 대상이면 현재는 대상 ID 문자열입니다.", example = "전체")
    String targetName,

    @Schema(description = "권한 출처. 사용자 직접 권한은 MANUAL, 부서 일반 부서원 권한은 MEMBER, 부서장 추가 권한은 MANAGER로 내려갑니다. 출처 구분이 없는 응답에서는 null일 수 있습니다.", example = "MANUAL", nullable = true)
    String source
) {
    public static final String SOURCE_MANUAL = "MANUAL";

    public PermissionResponse(String name, String code) {
        this(code == null ? empty(name, null) : from(null, code));
    }

    private PermissionResponse(PermissionResponse response) {
        this(
            response.id(),
            response.name(),
            response.code(),
            response.permissionCode(),
            response.resourceCode(),
            response.resourceLabel(),
            response.actionCode(),
            response.actionLabel(),
            response.scope(),
            response.targetId(),
            response.targetName(),
            response.source()
        );
    }

    public static PermissionResponse from(RoleType roleType) {
        return empty(roleType.name(), null);
    }

    private static PermissionResponse empty(String name, String code) {
        return new PermissionResponse(
            null,
            name,
            code,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public static PermissionResponse from(Long id, String authorityCode) {
        return from(id, authorityCode, null);
    }

    public static PermissionResponse from(Long id, String authorityCode, String source) {
        PermissionCode permissionCode = new PermissionCode(authorityCode);
        return new PermissionResponse(
            id,
            permissionCode.value(),
            permissionCode.value(),
            permissionCode.value(),
            permissionCode.resource().getCode(),
            PermissionRegistry.getResourceLabel(permissionCode.resource()),
            permissionCode.action().getCode(),
            PermissionRegistry.getActionLabel(permissionCode.action()),
            permissionCode.isGlobal() ? "global" : "target",
            permissionCode.targetId(),
            permissionCode.isGlobal() ? "전체" : String.valueOf(permissionCode.targetId()),
            source
        );
    }
}
