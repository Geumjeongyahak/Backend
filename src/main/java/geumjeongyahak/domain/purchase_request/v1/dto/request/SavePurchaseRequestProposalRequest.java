package geumjeongyahak.domain.purchase_request.v1.dto.request;

import geumjeongyahak.domain.purchase_request.enums.PurchaseBudgetItemCategory;
import geumjeongyahak.domain.purchase_request.enums.PurchaseCalculationDetail;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentAccount;
import geumjeongyahak.domain.purchase_request.v1.validation.ValidPurchaseProposalBudget;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "품의 정보 중간 저장 요청. 모든 필드는 선택값이며 전달된 전체 상태로 교체됩니다.")
public record SavePurchaseRequestProposalRequest(
    @Schema(description = "품의 개요", example = "7월 교재 구입 비용을 다음과 같이 지출하고자 합니다.", nullable = true)
    String overview,

    @Schema(description = "정책 사업 이름", example = "2026년 성인문해교육 지원사업", nullable = true)
    @Size(max = 255)
    String policyProject,

    @Schema(description = "단위 사업 이름", example = "프로그램운영비", nullable = true)
    @Size(max = 255)
    String unitProject,

    @Schema(description = "세부 사업 이름. 예산 내역에서도 동일한 값을 사용합니다.", example = "인건비", nullable = true)
    @Size(max = 255)
    String detailProject,

    @Schema(description = "요구 부서 ID. 미입력 시 null입니다.", example = "2", nullable = true)
    Long requestDepartmentId,

    @Schema(description = "품의일자. 오늘 또는 과거 날짜만 허용합니다.", example = "2026-07-25", nullable = true)
    @PastOrPresent(message = "품의일자는 미래일 수 없습니다.")
    LocalDate proposalDate,

    @Schema(description = "품의금액(원)", example = "20000", minimum = "0", nullable = true)
    @PositiveOrZero(message = "품의금액은 0원 이상이어야 합니다.")
    Long proposalAmount,

    @Schema(
        description = "결제 통장 코드. 품의일자와 함께 저장되면 서버가 품의번호를 계산합니다.",
        example = "NATIONAL_SUBSIDY_04",
        nullable = true
    )
    PurchasePaymentAccount paymentAccount,

    @Schema(description = "예산 내역. null이면 기존 예산 내역을 제거합니다.", nullable = true)
    @Valid
    BudgetRequest budget,

    @Schema(description = "품목 내역. null 또는 빈 배열이면 기존 품목을 모두 제거합니다.", nullable = true)
    List<@NotNull @Valid ItemRequest> items
) {
    @Schema(description = "품의 예산 내역")
    @ValidPurchaseProposalBudget
    public record BudgetRequest(
        @Schema(description = "세부 항목 코드", example = "TEXTBOOK", nullable = true)
        PurchaseBudgetItemCategory itemCategory,

        @Schema(
            description = "세부 항목 직접 입력값. itemCategory가 DIRECT_INPUT일 때만 입력합니다.",
            example = "시설 유지보수비",
            nullable = true
        )
        @Size(max = 255)
        String customItemCategory,

        @Schema(description = "산출 내역 코드", example = "COMMERCIAL_TEXTBOOK", nullable = true)
        PurchaseCalculationDetail calculationDetail,

        @Schema(
            description = "산출 내역 직접 입력값. calculationDetail이 DIRECT_INPUT일 때만 입력합니다.",
            example = "냉난방기 수리",
            nullable = true
        )
        @Size(max = 255)
        String customCalculationDetail
    ) {
    }

    @Schema(description = "품의 품목 내역")
    public record ItemRequest(
        @Schema(description = "품목 내용", example = "국어 교재", nullable = true)
        String content,

        @Schema(description = "품목 규격", example = "권", nullable = true)
        @Size(max = 255)
        String specification,

        @Schema(description = "수량", example = "2", minimum = "1", nullable = true)
        @Min(value = 1, message = "품목 수량은 1 이상이어야 합니다.")
        Integer quantity,

        @Schema(description = "예상 단가(원). 예상 금액은 서버에서 수량과 곱하여 계산합니다.", example = "8000", minimum = "0", nullable = true)
        @PositiveOrZero(message = "예상 단가는 0원 이상이어야 합니다.")
        Long estimatedUnitPrice
    ) {
        @AssertTrue(message = "수량과 예상 단가의 곱이 허용 금액 범위를 초과합니다.")
        public boolean isExpectedAmountWithinRange() {
            if (quantity == null || estimatedUnitPrice == null) {
                return true;
            }
            try {
                Math.multiplyExact(quantity.longValue(), estimatedUnitPrice);
                return true;
            } catch (ArithmeticException exception) {
                return false;
            }
        }
    }
}
