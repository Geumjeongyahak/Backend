package geumjeongyahak.domain.purchase_request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ReviewPurchaseRequestRequest(

    @NotBlank
    @Schema(description = "승인/반려 사유", example = "수업 운영에 필요한 물품으로 확인했습니다.")
    String note
) {}
