package geumjeongyahak.domain.daily_schedule.v1.dto.request;

import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;

@Getter
@Setter
public class DailySchedulePaginationRequest extends BasePaginationRequest {

    @Schema(description = "검색어. 분반명, 담당 교사명, 과목명, 수업 일지 내용에서 검색합니다.", example = "수학")
    private String keyword;

    @Schema(description = "true이면 로그인 사용자가 담당자인 하루 일정만 조회합니다.", example = "true")
    private Boolean mine;

    @Override
    public PageRequest toRequest() {
        return PageRequest.of(getPage(), getSize());
    }
}
