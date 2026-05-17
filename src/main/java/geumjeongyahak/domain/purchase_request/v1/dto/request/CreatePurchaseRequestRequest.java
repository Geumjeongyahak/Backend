package geumjeongyahak.domain.purchase_request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

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

    @Min(0)
    @Schema(description = "요청 전체 선금 요청 금액 (원, 선택)", example = "50000")
    Long advancePaymentRequestedAmount,

    @Valid
    @NotEmpty
    @Schema(description = "구입 항목 목록")
    List<Item> items,

    @Schema(description = "구입 요청에 첨부할 영수증 파일 ID 목록")
    List<UUID> receiptFileIds
) {
    public record Item(

        @NotBlank
        @Schema(description = "품명", example = "국어 교재")
        String name,

        @Schema(description = "구입 사유", example = "수업 교재 부족")
        String reason,

        @Min(0)
        @Schema(description = "예상 가격 (원, 선택)", example = "15000")
        Long expectedPrice
    ) {}
}
