package geumjeongyahak.domain.purchase_request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentType;

public record CreatePurchaseRequestRequest(

    @NotBlank
    @Schema(description = "구입 요청 제목", example = "교재 구입")
    String title,

    @NotBlank
    @Schema(description = "구입 요청 내용", example = "수업에 필요한 교재를 구입합니다.")
    String content,

    @NotNull
    @Schema(description = "구입 요청 대상 분반 ID", example = "1")
    Long classroomId,

    @Valid
    @NotEmpty
    @Schema(description = "결제 항목 목록")
    List<Item> items
) {
    public record Item(

        @NotBlank
        @Schema(description = "품명", example = "국어 교재")
        String name,

        @Schema(description = "구입 사유", example = "수업 교재 부족")
        String reason,

        @NotNull
        @Min(1)
        @Schema(description = "수량", example = "2")
        Integer quantity,

        @NotNull
        @Schema(description = "결제 유형", example = "PREPAID")
        PurchasePaymentType paymentType
    ) {}
}
