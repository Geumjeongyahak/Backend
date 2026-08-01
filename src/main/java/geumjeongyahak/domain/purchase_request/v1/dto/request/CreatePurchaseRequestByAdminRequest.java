package geumjeongyahak.domain.purchase_request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentType;
import geumjeongyahak.domain.purchase_request.v1.validation.PurchaseRequestTarget;
import geumjeongyahak.domain.purchase_request.v1.validation.ValidPurchaseRequestTarget;

@ValidPurchaseRequestTarget
public record CreatePurchaseRequestByAdminRequest(

    @NotNull
    @Schema(description = "실제 구입 요청자 사용자 ID", example = "3")
    Long requestedById,

    @NotBlank
    @Schema(description = "구입 요청 제목", example = "교재 구입")
    String title,

    @Schema(description = "구입 요청 내용", example = "수업에 필요한 교재를 구입합니다.", nullable = true)
    String content,

    @Schema(description = "구입 요청 대상 분반 ID. departmentId와 정확히 하나만 입력합니다.", example = "1", nullable = true)
    Long classroomId,

    @Schema(description = "구입 요청 대상 부서 ID. classroomId와 정확히 하나만 입력합니다.", example = "4", nullable = true)
    Long departmentId,

    @NotNull
    @Schema(description = "결제 유형", example = "PREPAID")
    PurchasePaymentType paymentType,

    @Valid
    @NotEmpty
    @Schema(description = "결제 항목 목록")
    List<Item> items
) implements PurchaseRequestTarget {
    public record Item(

        @NotBlank
        @Schema(description = "품명", example = "국어 교재")
        String name,

        @Schema(description = "구입 사유", example = "수업 교재 부족")
        String reason,

        @NotNull
        @Min(1)
        @Schema(description = "수량", example = "2")
        Integer quantity
    ) {}
}
