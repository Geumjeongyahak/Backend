package geumjeongyahak.domain.purchase_request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "품의 단계 영수증 첨부 요청")
public record AttachPurchaseRequestProposalReceiptRequest(
    @NotNull(message = "영수증 파일 ID는 필수입니다.")
    @Schema(description = "파일 업로드 API에서 발급받은 영수증 파일 ID")
    UUID fileId
) {
}
