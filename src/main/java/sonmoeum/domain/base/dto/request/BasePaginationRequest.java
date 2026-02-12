package sonmoeum.domain.base.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class BasePaginationRequest {
    @Schema(description = "페이지 번호", example = "0")

    @Min(value=0, message = "페이지 번호는 0 이상이어야 합니다.")
    int page;

    @Schema(description = "페이지 크기", example = "10")
    @Min(value=1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value=100, message = "페이지 크기는 100 이하이어야 합니다.")
    int size;

    public abstract PageRequest toRequest();

    public BasePaginationRequest() {
        this.page = 0;
        this.size = 10;
    }

    public BasePaginationRequest(
            Integer page,
            Integer size
    ) {
        this.page = (page == null || page < 0) ? 0 : page;
        this.size = (size == null || size <= 0) ? 10 : size;
    }

    protected List<Sort.Order> toSortOrders(String sortFields) {
        if (sortFields == null || sortFields.isEmpty()) {
            return List.of();
        }
        List<Sort.Order> orders = new ArrayList<>();
        String[] sorts = sortFields.split(";");
        for (String sort : sorts) {
            if (sort.isEmpty()) {
                continue;
            }
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            String direction = parts[1].trim().toUpperCase();
            if (direction.equals("ASC")) {
                orders.add(Sort.Order.asc(field));
            } else if (direction.equals("DESC")) {
                orders.add(Sort.Order.desc(field));
            }
        }
        return orders;
    }
}
