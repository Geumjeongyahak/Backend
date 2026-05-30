package geumjeongyahak.domain.event.v1.dto.request;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import geumjeongyahak.common.validation.annotation.ValidEventSearchCondition;
import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ValidEventSearchCondition
@Schema(description = "행사 목록 조회 요청")
public class EventSearchRequest extends BasePaginationRequest {

    public EventSearchRequest() {
        super(0, 20);
    }

    @Schema(description = "조회 시작일", example = "2026-05-01")
    private LocalDate startDate;

    @Schema(description = "조회 종료일", example = "2026-05-31")
    private LocalDate endDate;

    @Override
    public PageRequest toRequest() {
        return PageRequest.of(
            getPage(),
            getSize(),
            Sort.by(List.of(
                Sort.Order.asc("eventDate"),
                Sort.Order.asc("startTime"),
                Sort.Order.asc("id")
            ))
        );
    }
}
