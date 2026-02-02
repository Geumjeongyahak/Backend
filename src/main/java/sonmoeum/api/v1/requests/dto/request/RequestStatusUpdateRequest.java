package sonmoeum.api.v1.requests.dto.request;

import sonmoeum.domain.request.enums.RequestStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RequestStatusUpdateRequest(
    @Schema(description = "요청 상태 (APPROVED / REJECTED)", example = "APPROVED")
    @NotNull(message = "상태 값은 필수입니다.")
    RequestStatus status,

    @Schema(description = "비고 (반려 시 사유 등)", example = "승인합니다.")
    String note
) {}
