package sonmoeum.api.v1.requests.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePurchaseRequest(
    @Schema(description = "과목 ID", example = "1")
    @NotNull(message = "과목 ID는 필수입니다.")
    Long subjectId,

    @Schema(description = "제목", example = "기자재 구입 요청")
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
    String title,

    @Schema(description = "내용", example = "수업에 필요한 교구 구입")
    @NotBlank(message = "내용은 필수입니다.")
    String content,

    @Schema(description = "가격", example = "50000")
    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    Long price
) {}
