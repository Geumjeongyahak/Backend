package sonmoeum.domain.request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RejectRequestRequest(

    @NotBlank
    @Schema(description = "반려 사유", example = "사유가 충분하지 않습니다.")
    String note
) {}
