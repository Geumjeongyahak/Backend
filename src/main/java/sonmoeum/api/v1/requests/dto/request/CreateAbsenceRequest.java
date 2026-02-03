package sonmoeum.api.v1.requests.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAbsenceRequest(
    @Schema(description = "수업 ID", example = "1")
    @NotNull(message = "수업 ID는 필수입니다.")
    Long lessonId,

    @Schema(description = "결석 사유", example = "개인 사정으로 인한 결석")
    @NotBlank(message = "결석 사유는 필수입니다.")
    String reason
) {}
