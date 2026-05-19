package geumjeongyahak.domain.vendor.v1.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChargeVendorRequest(

    @NotNull
    @Min(1)
    @Schema(description = "충전 금액", example = "100000")
    Long amount,

    @Schema(description = "충전 메모", example = "5월 문구류 선결제 충전")
    String memo,

    @Schema(description = "충전 영수증 파일 ID")
    UUID receiptFileId
) {
}
