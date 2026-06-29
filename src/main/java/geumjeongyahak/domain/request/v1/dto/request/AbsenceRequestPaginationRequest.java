package geumjeongyahak.domain.request.v1.dto.request;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbsenceRequestPaginationRequest extends BasePaginationRequest {

    @Schema(description = "제목, 사유, 작성자 이름, 반 이름 검색어", example = "개인 사정")
    private String keyword;

    @Schema(description = "본인 요청만 조회할지 여부", example = "false")
    private boolean mine = false;

    @Override
    public PageRequest toRequest() {
        return PageRequest.of(
            getPage(),
            getSize(),
            Sort.by(List.of(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            ))
        );
    }
}
