package geumjeongyahak.domain.purchase_request.v1.dto.request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentType;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestForm {

    @NotNull(message = "분반은 필수입니다.")
    private Long classroomId;

    @NotNull(message = "담당 부서는 필수입니다.")
    private Long departmentId;

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String content;

    @NotNull(message = "결제 유형은 필수입니다.")
    private PurchasePaymentType paymentType = PurchasePaymentType.ACTUAL;

    @NotEmpty(message = "최소 하나 이상의 항목이 필요합니다.")
    private List<ItemForm> items = new ArrayList<>();

    private Long vendorId;
    private Long amount;
    private PurchasePaymentMethod paymentMethod;
    private UUID receiptFileId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemForm {
        private Long id;
        @NotBlank(message = "품명은 필수입니다.")
        private String name;
        private String reason;
        private Integer quantity = 1;
        private Long vendorId;
        private Long actualAmount;
        private UUID receiptFileId;
    }
}
