package sonmoeum.domain.request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePurchaseRequestRequest(

    @NotNull
    @Schema(description = "관련 과목 ID", example = "1")
    Long subjectId,

    @NotBlank
    @Schema(description = "구입 항목 제목", example = "교재 구입")
    String title,

    @NotBlank
    @Schema(description = "구입 요청 내용", example = "수업에 필요한 교재를 구입합니다.")
    String content,

    @NotNull
    @Min(1)
    @Schema(description = "가격 (원)", example = "15000")
    Long price
) {}
