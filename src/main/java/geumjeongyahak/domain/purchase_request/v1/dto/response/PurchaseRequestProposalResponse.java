package geumjeongyahak.domain.purchase_request.v1.dto.response;

import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposal;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalApprovalLine;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalBudget;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalItem;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestProposalReceipt;
import geumjeongyahak.domain.purchase_request.enums.PurchaseBudgetItemCategory;
import geumjeongyahak.domain.purchase_request.enums.PurchaseCalculationDetail;
import geumjeongyahak.domain.purchase_request.enums.PurchaseDocumentApprovalType;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "품의 정보 응답")
public record PurchaseRequestProposalResponse(
    @Schema(description = "품의 정보 ID", example = "1")
    Long id,

    @Schema(
        description = "현재 유효한 신청을 기준으로 계산한 품의번호. 품의일자나 결제 통장이 없으면 null입니다.",
        example = "2026품-국비04-01",
        nullable = true
    )
    String proposalNumber,

    @Schema(description = "품의서 제목. null이면 결제 신청 제목을 사용합니다.", example = "7월 교재 구입", nullable = true)
    String proposalTitle,

    @Schema(description = "결의서 지출명·내용·세부내역. null이면 결제 신청 제목을 사용합니다.", example = "7월 교재 구입", nullable = true)
    String resolutionTitle,

    @Schema(description = "완료 요청일", example = "2026-07-26", nullable = true)
    LocalDate completionDate,

    @Schema(description = "저장된 품의서 결재라인")
    List<ApprovalLineResponse> draftApprovals,

    @Schema(description = "저장된 품의서 협조라인")
    List<ApprovalLineResponse> draftCooperations,

    @Schema(description = "저장된 결의서 결재라인")
    List<ApprovalLineResponse> resolutionApprovals,

    @Schema(description = "품의 개요", example = "7월 교재 구입 비용을 다음과 같이 지출하고자 합니다.", nullable = true)
    String overview,

    @Schema(description = "정책 사업 이름", example = "2026년 성인문해교육 지원사업", nullable = true)
    String policyProject,

    @Schema(description = "단위 사업 이름", example = "프로그램운영비", nullable = true)
    String unitProject,

    @Schema(description = "세부 사업 이름", example = "인건비", nullable = true)
    String detailProject,

    @Schema(description = "요구 부서 ID", example = "2", nullable = true)
    Long requestDepartmentId,

    @Schema(description = "요구 부서 이름", example = "총무부", nullable = true)
    String requestDepartmentName,

    @Schema(description = "품의일자", example = "2026-07-25", nullable = true)
    LocalDate proposalDate,

    @Schema(description = "품의금액(원)", example = "20000", nullable = true)
    Long proposalAmount,

    @Schema(description = "결제 통장 코드", example = "NATIONAL_SUBSIDY_04", nullable = true)
    PurchasePaymentAccount paymentAccount,

    @Schema(description = "예산 내역", nullable = true)
    BudgetResponse budget,

    @Schema(description = "품목 내역. 품목이 없으면 빈 배열입니다.")
    List<ItemResponse> items,

    @Schema(description = "삭제되지 않은 품의 단계 영수증 목록")
    List<ReceiptResponse> receipts,

    @Schema(description = "품의 정보 최초 저장 시각")
    LocalDateTime createdAt,

    @Schema(description = "품의 정보 최종 수정 시각")
    LocalDateTime updatedAt
) {
    @Schema(description = "문서 결재·협조 항목 응답")
    public record ApprovalLineResponse(
        @Schema(description = "직위", example = "총무", nullable = true)
        String position,

        @Schema(description = "이름", example = "관리자", nullable = true)
        String name,

        @Schema(description = "표시 순서. 0부터 시작합니다.", example = "0")
        int sortOrder
    ) {
        static ApprovalLineResponse from(PurchaseRequestProposalApprovalLine line) {
            return new ApprovalLineResponse(line.getPosition(), line.getName(), line.getSortOrder());
        }
    }

    @Schema(description = "품의 예산 내역 응답")
    public record BudgetResponse(
        @Schema(description = "예산 내역 ID", example = "1")
        Long id,

        @Schema(description = "세부 항목 코드", example = "TEXTBOOK", nullable = true)
        PurchaseBudgetItemCategory itemCategory,

        @Schema(description = "세부 항목 직접 입력값", example = "시설 유지보수비", nullable = true)
        String customItemCategory,

        @Schema(description = "산출 내역 코드", example = "COMMERCIAL_TEXTBOOK", nullable = true)
        PurchaseCalculationDetail calculationDetail,

        @Schema(description = "산출 내역 직접 입력값", example = "냉난방기 수리", nullable = true)
        String customCalculationDetail
    ) {
        static BudgetResponse from(PurchaseRequestProposalBudget budget) {
            return new BudgetResponse(
                budget.getId(),
                budget.getItemCategory(),
                budget.getCustomItemCategory(),
                budget.getCalculationDetail(),
                budget.getCustomCalculationDetail()
            );
        }
    }

    @Schema(description = "품의 품목 내역 응답")
    public record ItemResponse(
        @Schema(description = "품목 내역 ID", example = "1")
        Long id,

        @Schema(description = "품목 내용", example = "국어 교재", nullable = true)
        String content,

        @Schema(description = "품목 규격", example = "권", nullable = true)
        String specification,

        @Schema(description = "수량", example = "2", nullable = true)
        Integer quantity,

        @Schema(description = "예상 단가(원)", example = "8000", nullable = true)
        Long estimatedUnitPrice,

        @Schema(description = "수량과 예상 단가로 계산한 예상 금액(원)", example = "16000", nullable = true)
        Long expectedAmount,

        @Schema(description = "품목 표시 순서. 0부터 시작합니다.", example = "0")
        int sortOrder
    ) {
        static ItemResponse from(PurchaseRequestProposalItem item) {
            return new ItemResponse(
                item.getId(),
                item.getContent(),
                item.getSpecification(),
                item.getQuantity(),
                item.getEstimatedUnitPrice(),
                item.calculateExpectedAmount(),
                item.getSortOrder()
            );
        }
    }

    @Schema(description = "품의 단계 영수증 응답")
    public record ReceiptResponse(
        @Schema(description = "품의 단계 영수증 연결 ID", example = "1")
        Long id,

        @Schema(description = "파일 ID")
        UUID fileId,

        @Schema(description = "원본 파일 이름", example = "receipt.png", nullable = true)
        String originalName,

        @Schema(description = "파일 MIME 타입", example = "image/png")
        String contentType,

        @Schema(description = "파일 공개 URL", nullable = true)
        String fileUrl,

        @Schema(description = "영수증 표시 순서. 0부터 시작합니다.", example = "0")
        int sortOrder,

        @Schema(description = "영수증 첨부 시각")
        LocalDateTime createdAt
    ) {
        static ReceiptResponse from(PurchaseRequestProposalReceipt receipt) {
            return new ReceiptResponse(
                receipt.getId(),
                receipt.getFile().getId(),
                receipt.getFile().getOriginalName(),
                receipt.getFile().getContentType(),
                receipt.getFile().getPublicUrl(),
                receipt.getSortOrder(),
                receipt.getCreatedAt()
            );
        }
    }

    public static PurchaseRequestProposalResponse from(PurchaseRequestProposal proposal, String proposalNumber) {
        return new PurchaseRequestProposalResponse(
            proposal.getId(),
            proposalNumber,
            proposal.getProposalTitle(),
            proposal.getResolutionTitle(),
            proposal.getCompletionDate(),
            approvalLines(proposal, PurchaseDocumentApprovalType.DRAFT_APPROVAL),
            approvalLines(proposal, PurchaseDocumentApprovalType.DRAFT_COOPERATION),
            approvalLines(proposal, PurchaseDocumentApprovalType.RESOLUTION_APPROVAL),
            proposal.getOverview(),
            proposal.getPolicyProject(),
            proposal.getUnitProject(),
            proposal.getDetailProject(),
            proposal.getRequestDepartment() != null ? proposal.getRequestDepartment().getId() : null,
            proposal.getRequestDepartment() != null ? proposal.getRequestDepartment().getName() : null,
            proposal.getProposalDate(),
            proposal.getProposalAmount(),
            proposal.getPaymentAccount(),
            proposal.getBudget() != null ? BudgetResponse.from(proposal.getBudget()) : null,
            proposal.getItems().stream().map(ItemResponse::from).toList(),
            proposal.getReceipts().stream()
                .filter(receipt -> !receipt.isDeleted() && !receipt.getFile().isDeleted())
                .map(ReceiptResponse::from)
                .toList(),
            proposal.getCreatedAt(),
            proposal.getUpdatedAt()
        );
    }

    private static List<ApprovalLineResponse> approvalLines(
        PurchaseRequestProposal proposal,
        PurchaseDocumentApprovalType type
    ) {
        return proposal.getApprovalLines().stream()
            .filter(line -> line.getLineType() == type)
            .map(ApprovalLineResponse::from)
            .toList();
    }
}
