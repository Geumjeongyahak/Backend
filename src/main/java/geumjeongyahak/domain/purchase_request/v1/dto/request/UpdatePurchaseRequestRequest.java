package geumjeongyahak.domain.purchase_request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import geumjeongyahak.domain.purchase_request.v1.validation.PurchaseRequestTarget;
import geumjeongyahak.domain.purchase_request.v1.validation.ValidPurchaseRequestTarget;

@ValidPurchaseRequestTarget
public record UpdatePurchaseRequestRequest(

    @Schema(description = "구입 요청 대상 분반 ID. departmentId와 정확히 하나만 입력합니다.", example = "1", nullable = true)
    Long classroomId,

    @Schema(description = "구입 요청 대상 부서 ID. classroomId와 정확히 하나만 입력합니다.", example = "4", nullable = true)
    Long departmentId,

    @NotBlank
    @Schema(description = "구입 요청 제목", example = "교재 구입")
    String title,

    @Schema(description = "구입 요청 내용", example = "수업에 필요한 교재를 구입합니다.", nullable = true)
    String content,

    @Valid
    @NotEmpty
    @Schema(description = "결제 항목 목록")
    List<CreatePurchaseRequestRequest.Item> items
) implements PurchaseRequestTarget {
}
