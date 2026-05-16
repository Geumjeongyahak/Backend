package geumjeongyahak.domain.request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAbsenceRequestRequest(

    @NotNull
    @Schema(description = "결석할 수업 ID", example = "1")
    Long lessonId,

    @NotBlank
    @Schema(description = "결석 요청 제목", example = "개인 사정으로 결석합니다")
    String title,

    @NotBlank
    @Schema(description = "결석 사유", example = "개인 사정으로 인한 결석")
    String reason
) {}
