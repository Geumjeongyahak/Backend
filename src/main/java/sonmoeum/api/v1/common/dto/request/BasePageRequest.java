package sonmoeum.api.v1.common.dto.request;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ParameterObject
public class BasePageRequest {

    @Schema(description = "페이지 번호 (1부터 시작)", example = "1")
    private Integer page;

    @Schema(description = "페이지 크기", example = "10")
    private Integer size;

    public BasePageRequest(Integer page, Integer size) {
        this.page = (page == null) ? 1 : page;
        this.size = (size == null) ? 10 : size;
    }

    @NonNull
    public PageRequest toPageRequest() {
        return PageRequest.of(this.page - 1, this.size);
    }
}
