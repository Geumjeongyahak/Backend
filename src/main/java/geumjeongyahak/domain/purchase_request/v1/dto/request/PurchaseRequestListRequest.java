package geumjeongyahak.domain.purchase_request.v1.dto.request;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import geumjeongyahak.common.validation.annotation.ValidSortField;
import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "구입 요청 목록 조회 요청")
public class PurchaseRequestListRequest extends BasePaginationRequest {

    @Schema(description = "구입 요청 상태")
    private PurchaseRequestStatus status;

    @Schema(description = "본인 요청만 조회 여부", example = "false")
    private boolean mine = false;

    @Schema(description = "제목, 분반명, 작성자명 통합 검색어")
    private String keyword;

    @Schema(description = "분반명 검색어")
    private String classroomName;

    @Schema(description = "작성자명 검색어")
    private String requestedByName;

    @Schema(description = "정렬 기준. 예: createdAt,DESC 또는 classroomName,ASC;createdAt,DESC")
    @ValidSortField(fields = {
        "id",
        "title",
        "classroomName",
        "requestedByName",
        "totalPrice",
        "status",
        "createdAt",
        "updatedAt"
    })
    private String sort;

    @Override
    public PageRequest toRequest() {
        return PageRequest.of(getPage(), getSize(), Sort.by(toSortOrdersOrDefault()));
    }

    private List<Sort.Order> toSortOrdersOrDefault() {
        List<Sort.Order> orders = toSortOrders(sort);
        if (orders.isEmpty()) {
            return List.of(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        }

        return orders.stream()
            .map(order -> order.withProperty(toEntitySortProperty(order.getProperty())))
            .toList();
    }

    private String toEntitySortProperty(String property) {
        return switch (property) {
            case "classroomName" -> "classroom.name";
            case "requestedByName" -> "requestedBy.name";
            default -> property;
        };
    }
}
