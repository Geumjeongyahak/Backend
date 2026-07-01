package geumjeongyahak.domain.purchase_request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import geumjeongyahak.domain.purchase_request.enums.ExpenseDocumentPaymentMethod;

@Schema(description = "지출증빙서류 생성 요청")
public record GenerateExpenseDocumentRequest(

    @Schema(description = "회계연도", example = "2026년")
    String fiscalYear,

    @Schema(description = "품의번호", example = "2026품-목민서관-01")
    String draftDocumentNumber,

    @Schema(description = "지출번호", example = "2026결-목민서관-01")
    String resolutionDocumentNumber,

    @Schema(description = "정책사업명", example = "성인문해교육 지원사업")
    String policyProject,

    @Schema(description = "단위사업명", example = "교재비")
    String unitProject,

    @Schema(description = "세부사업명", example = "사업추진비")
    String detailProject,

    @Schema(description = "예산내역 산출내역", example = "문해 교재")
    String budgetDetail,

    @Schema(description = "예산잔액", example = "100000")
    Long budgetBalance,

    @Schema(description = "사업잔액", example = "500000")
    Long projectBalance,

    @Schema(description = "요구부서", example = "교육연구부")
    String requestDepartment,

    @Schema(description = "품의일자", example = "2026. 06. 30.")
    String draftDate,

    @Schema(description = "완료 요청일", example = "2026. 06. 30.")
    String completionDate,

    @Schema(description = "수신처", example = "목민서관")
    String receiver,

    @Schema(description = "지급구분", example = "TRANSFER")
    ExpenseDocumentPaymentMethod paymentMethod,

    @Schema(description = "발의일자", example = "2026. 06. 30.")
    String initiationDate,

    @Schema(description = "결의일자", example = "2026. 06. 30.")
    String resolutionDate,

    @Schema(description = "지급일자", example = "2026. 06. 30.")
    String paymentDate,

    @Schema(description = "은행계좌", example = "국민은행 000000-00-000000")
    String bankAccount,

    @Schema(description = "사업자번호", example = "000-00-00000")
    String businessNumber,

    @Schema(description = "예금주", example = "금정열린배움터")
    String accountHolder,

    @Schema(description = "비고")
    String note,

    @Schema(description = "품목별 지출증빙서류 보완 입력값. 구매 요청 품목 순서와 같은 순서로 매칭됩니다.")
    List<ExpenseDocumentItem> items,

    @Schema(description = "품의서 신청 결재라인")
    List<ApprovalLine> draftApprovals,

    @Schema(description = "품의서 협조라인")
    List<ApprovalLine> draftCooperations,

    @Schema(description = "결의서 확인 결재라인")
    List<ApprovalLine> resolutionApprovals
) {
    @Schema(description = "문서 결재라인 항목")
    public record ApprovalLine(

        @Schema(description = "직위", example = "담당")
        String position,

        @Schema(description = "이름", example = "김담당")
        String name
    ) {
    }

    @Schema(description = "지출증빙서류 품목 보완 입력값")
    public record ExpenseDocumentItem(

        @Schema(description = "규격", example = "A4")
        String spec,

        @Schema(description = "예상단가", example = "3000")
        Long unitPrice
    ) {
    }
}
