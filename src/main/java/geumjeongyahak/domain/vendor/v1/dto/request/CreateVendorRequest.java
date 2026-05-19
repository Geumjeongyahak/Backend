package geumjeongyahak.domain.vendor.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateVendorRequest(

    @NotBlank
    @Schema(description = "거래처명", example = "금정문구")
    String name,

    @Schema(description = "거래처 설명", example = "문구류 선결제 거래처")
    String description
) {
}
