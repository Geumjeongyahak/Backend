package sonmoeum.api.v1.common.dto.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class BasePageResponse<T> {
    @Schema(description = "전체 데이터 수", example = "100")
    private Long totalCount;
    
    @Schema(description = "전체 페이지 수", example = "10")
    private Integer totalPages;
    
    @Schema(description = "현재 페이지 번호", example = "1")
    private Integer currentPage;
    
    @Schema(description = "페이지 크기", example = "10")
    private Integer pageSize;
    
    @Schema(description = "데이터 목록")
    private List<T> content;
    
    public static <T> BasePageResponse<T> from(
        Page<T> pageData
    ) {
        BasePageResponse<T> response = new BasePageResponse<>();
        response.setTotalCount(pageData.getTotalElements());
        response.setTotalPages(pageData.getTotalPages());
        response.setCurrentPage(pageData.getNumber() + 1);
        response.setPageSize(pageData.getSize());
        response.setContent(pageData.getContent());
        return response;
    }

    public <F> BasePageResponse<F> convertTo(
        Function<T, F> mapper
    ) {
        BasePageResponse<F> response = new BasePageResponse<>();
        response.setTotalCount(this.totalCount);
        response.setTotalPages(this.totalPages);
        response.setCurrentPage(this.currentPage);
        response.setPageSize(this.pageSize);
        response.setContent(
            this.content.stream()
                .map(mapper)
                .toList()
        );
        return response;
    }
}
