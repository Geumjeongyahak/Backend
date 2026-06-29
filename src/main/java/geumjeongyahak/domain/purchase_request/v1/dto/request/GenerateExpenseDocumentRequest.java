package geumjeongyahak.domain.purchase_request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지출증빙서류 생성 요청")
public record GenerateExpenseDocumentRequest(

    @Schema(description = "회계연도", example = "2026년")
    String fiscalYear,

    @Schema(description = "품의번호", example = "2026품-구비01-01")
    String draftDocumentNumber,

    @Schema(description = "지출번호", example = "2026결-구비01-01")
    String resolutionDocumentNumber,

    @Schema(description = "정책사업명", example = "성인문해교육 지원사업")
    String policyProject,

    @Schema(description = "단위사업명", example = "프로그램운영비")
    String unitProject,

    @Schema(description = "세부사업명", example = "사업추진비")
    String detailProject,

    @Schema(description = "요구부서", example = "교육연구부")
    String requestDepartment,

    @Schema(description = "완료 요청일", example = "2026. 06. 30.")
    String completionDate,

    @Schema(description = "수신처", example = "금정열린배움터 교장")
    String receiver,

    @Schema(description = "지급구분", example = "법인카드")
    String paymentMethod,

    @Schema(description = "발의일자", example = "2026. 06. 29.")
    String initiationDate,

    @Schema(description = "결의일자", example = "2026. 06. 29.")
    String resolutionDate,

    @Schema(description = "지급일자", example = "2026. 06. 29.")
    String paymentDate,

    @Schema(description = "은행계좌", example = "국민은행 000000-00-000000")
    String bankAccount,

    @Schema(description = "사업자번호", example = "000-00-00000")
    String businessNumber,

    @Schema(description = "예금주", example = "금정열린배움터")
    String accountHolder,

    @Schema(description = "담당자", example = "홍길동")
    String manager,

    @Schema(description = "연락처", example = "010-0000-0000")
    String contact,

    @Schema(description = "비고")
    String note
) {
}
