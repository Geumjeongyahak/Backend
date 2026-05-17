package geumjeongyahak.domain.request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAbsenceRequestRequest(

    @NotNull
    @Schema(description = "결석할 하루 일정 ID", example = "1")
    Long dailyScheduleId,

    @NotBlank
    @Schema(description = "결석 사유", example = "개인 사정으로 인한 결석")
    String reason
) {}
