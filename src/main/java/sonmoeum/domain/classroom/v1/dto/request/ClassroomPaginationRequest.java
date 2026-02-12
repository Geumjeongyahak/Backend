package sonmoeum.domain.classroom.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import sonmoeum.common.validation.annotation.ValidSortField;
import sonmoeum.domain.base.dto.request.BasePaginationRequest;

@Getter
@Setter
public class ClassroomPaginationRequest extends BasePaginationRequest {

    @Schema(description = "분반 이름의 일부 또는 전체 기반 필터링", example = "해바라기")
    private String name;

    @Schema(description = "분반 유형 기반 필터링", example = "WEEKDAY")
    private String type;

    @Schema(description = "정렬 기준 필드와 방향 쌍의 리스트", example = "[\"name\", \"ASC\"; \"createdAt\", \"DESC\"]")
    @ValidSortField(fields = {"id", "name", "createdAt", "updatedAt"})
    private String sort;

    public ClassroomPaginationRequest() {
        super();
    }

    @Override
    public PageRequest toRequest() {
        return PageRequest.of(
                this.getPage(),
                this.getSize(),
                Sort.by(toSortOrders(this.sort))
        );
    }
}
