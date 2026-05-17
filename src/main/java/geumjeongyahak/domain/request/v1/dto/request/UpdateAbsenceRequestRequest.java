package geumjeongyahak.domain.request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateAbsenceRequestRequest(

    @NotBlank
    @Schema(description = "결석 요청 제목", example = "개인 사정으로 결석합니다")
    String title,

    @NotBlank
    @Schema(description = "결석 사유", example = "개인 사정으로 인한 결석")
    String reason
) {}
