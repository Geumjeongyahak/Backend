package geumjeongyahak.domain.request.v1.dto.request;

import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class LessonExchangeRequestListRequest extends BasePaginationRequest {

    @Schema(description = "조회할 요청 상태", example = "APPROVED")
    private LessonExchangeRequestStatus status;

    @Schema(description = "본인 요청만 조회할지 여부", example = "false")
    private boolean mine = false;

    @Override
    public PageRequest toRequest() {
        return PageRequest.of(
            this.getPage(),
            this.getSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
}
