package geumjeongyahak.domain.purchase_request.v1.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestForm {

    private Long classroomId;

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @NotEmpty(message = "최소 하나 이상의 항목이 필요합니다.")
    private List<ItemForm> items = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemForm {
        private Long id;
        @NotBlank(message = "품명은 필수입니다.")
        private String name;
        private String reason;
        private Long price;
        private java.util.UUID receiptFileId;
        private String receiptFileUrl; // For display/preview
    }
}
