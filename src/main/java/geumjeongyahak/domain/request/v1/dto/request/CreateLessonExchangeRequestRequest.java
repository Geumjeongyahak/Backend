package geumjeongyahak.domain.request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLessonExchangeRequestRequest(

    @NotNull
    @Schema(description = "교환 대상 수업 ID", example = "1")
    Long lessonId,

    @NotBlank
    @Schema(description = "요청 제목", example = "수업 교환 요청")
    String title,

    @NotBlank
    @Schema(description = "요청 내용", example = "사정으로 인해 교환을 요청합니다.")
    String content
) {}
