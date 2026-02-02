package sonmoeum.api.v1.requests.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExchangeRequest(
    @Schema(description = "대상 ID (수업 교환 시 lessonId, 과목 교환 시 subjectId)", example = "1")
    @NotNull(message = "대상 ID는 필수입니다.")
    Long targetId,

    @Schema(description = "제목", example = "수업 교환 요청합니다.")
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
    String title,

    @Schema(description = "내용", example = "사정이 생겨 교환 요청합니다.")
    @NotBlank(message = "내용은 필수입니다.")
    String content
) {}
