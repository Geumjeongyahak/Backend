package geumjeongyahak.domain.purchase_request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReportPurchaseRequest(

    @Valid
    @NotEmpty
    @Schema(description = "구매 완료 항목 목록")
    List<ItemReport> items,

    @Schema(description = "구매 요청 단위 영수증 파일 ID 목록")
    List<UUID> receiptFileIds
) {
    public record ItemReport(

        @NotNull
        @Schema(description = "항목 ID", example = "1")
        Long itemId,

        @NotNull
        @Min(0)
        @Schema(description = "실 구매 단가 (원)", example = "15000")
        Long price
    ) {}
}
