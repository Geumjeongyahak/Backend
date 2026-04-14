package geumjeongyahak.domain.request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSubjectExchangeRequestRequest(

    @NotNull
    @Schema(description = "교환 대상 과목 ID", example = "1")
    Long subjectId,

    @NotBlank
    @Schema(description = "요청 제목", example = "과목 교환 요청")
    String title,

    @NotBlank
    @Schema(description = "요청 내용", example = "과목 교환을 요청합니다.")
    String content
) {}
