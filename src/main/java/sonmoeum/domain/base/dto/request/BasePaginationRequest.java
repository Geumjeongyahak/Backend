package sonmoeum.domain.base.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;

@Getter
@Setter
public abstract class BasePaginationRequest {
    @Schema(description = "페이지 번호", example = "0")
    int page;
    @Schema(description = "페이지 크기", example = "10")
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
}
