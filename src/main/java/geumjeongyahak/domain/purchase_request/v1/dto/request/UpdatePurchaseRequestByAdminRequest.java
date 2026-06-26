package geumjeongyahak.domain.purchase_request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpdatePurchaseRequestByAdminRequest(

    @NotBlank
    @Schema(description = "구입 요청 제목", example = "교재 구입")
    String title,

    @NotBlank
    @Schema(description = "구입 요청 내용", example = "수업에 필요한 교재를 구입합니다.")
    String content,

    @Valid
    @NotEmpty
    @Schema(description = "결제 항목 목록")
    List<CreatePurchaseRequestRequest.Item> items
) {
}
