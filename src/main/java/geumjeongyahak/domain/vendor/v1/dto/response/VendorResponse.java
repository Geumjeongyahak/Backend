package geumjeongyahak.domain.vendor.v1.dto.response;

import java.time.LocalDateTime;

import geumjeongyahak.domain.vendor.entity.Vendor;
import io.swagger.v3.oas.annotations.media.Schema;

public record VendorResponse(
    @Schema(description = "거래처 ID", example = "1")
    Long id,

    @Schema(description = "거래처명", example = "금정문구")
    String name,

    @Schema(description = "거래처 설명")
    String description,

    @Schema(description = "현재 잔액", example = "85000")
    Long balance,

    @Schema(description = "활성 여부", example = "true")
    boolean isActive,

    @Schema(description = "생성 시각")
    LocalDateTime createdAt
) {
    public static VendorResponse from(Vendor vendor) {
        return new VendorResponse(
            vendor.getId(),
            vendor.getName(),
            vendor.getDescription(),
            vendor.getBalance(),
            vendor.isActive(),
            vendor.getCreatedAt()
        );
    }
}
