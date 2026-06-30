package geumjeongyahak.domain.users.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidPermissionCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 직접 권한 추가/삭제 요청 DTO. 하나의 permission code를 대상으로 동작합니다.")
public record UserPermissionRequest(
    @Schema(
        description = "추가하거나 제거할 permission code. 현재 권한 정책에서 허용하는 authority 문자열 형식을 따라야 합니다.",
        example = "user:manage:*"
    )
    @ValidPermissionCode
    String permissionCode
) {
}
