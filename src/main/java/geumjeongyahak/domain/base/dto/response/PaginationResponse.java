package geumjeongyahak.domain.base.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.function.Function;

@Getter
public class PaginationResponse<T> {
    @Schema(description = "페이지 내용")
    private List<T> content;

    @Schema(description = "현재 페이지 번호", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "10")
    private int size;

    @Schema(description = "전체 요소 수", example = "100")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "10")
    private int totalPages;

    private PaginationResponse() {

    }

    public PaginationResponse(@NonNull Page<T> pageData) {
        this.content = pageData.getContent();
        this.page = pageData.getNumber();
        this.size = pageData.getSize();
        this.totalElements = pageData.getTotalElements();
        this.totalPages = pageData.getTotalPages();
    }

    public static <T,F> PaginationResponse<F> from(
            Page<T> pageData,
            Function<T,F> mapper
    ) {
        PaginationResponse<F> mapped = new PaginationResponse<>();
        mapped.content = pageData.getContent().stream()
            .map(mapper)
            .toList();
        mapped.page = pageData.getNumber();
        mapped.size = pageData.getSize();
        mapped.totalElements = pageData.getTotalElements();
        mapped.totalPages = pageData.getTotalPages();
        return mapped;
    }

    public static <T,F> PaginationResponse<F> mapTo(
        PaginationResponse<T> original,
        Function<T,F> mapper
    ) {
        PaginationResponse<F> mapped = new PaginationResponse<>();
        mapped.content = original.getContent().stream()
            .map(mapper)
            .toList();
        mapped.page = original.getPage();
        mapped.size = original.getSize();
        mapped.totalElements = original.getTotalElements();
        mapped.totalPages = original.getTotalPages();
        return mapped;
    }
}
