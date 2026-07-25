package geumjeongyahak.domain.purchase_request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentMethod;

public record ReportPurchaseRequest(

    @Valid
    @NotEmpty
    @Schema(description = "구매 완료 거래 목록")
    List<TransactionReport> transactions
) {
    public record TransactionReport(

        @NotNull
        @Schema(description = "거래처 ID", example = "1")
        Long vendorId,

        @NotEmpty
        @Schema(description = "영수증 하나에 포함된 품목명 목록")
        List<@NotBlank String> itemNames,

        @NotNull
        @Min(1)
        @Schema(description = "총 결제 금액", example = "15000")
        Long amount,

        @Schema(description = "지급 구분(선금 결제 시 필수)", example = "CARD")
        PurchasePaymentMethod paymentMethod,

        @Schema(description = "영수증 파일 ID")
        UUID receiptFileId
    ) {}
}
