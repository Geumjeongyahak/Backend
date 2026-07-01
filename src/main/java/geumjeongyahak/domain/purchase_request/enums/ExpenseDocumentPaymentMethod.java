package geumjeongyahak.domain.purchase_request.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지출증빙서류 지급구분")
public enum ExpenseDocumentPaymentMethod {
    CASH,
    CARD,
    TRANSFER,
    AUTO_TRANSFER,
    OTHER
}
