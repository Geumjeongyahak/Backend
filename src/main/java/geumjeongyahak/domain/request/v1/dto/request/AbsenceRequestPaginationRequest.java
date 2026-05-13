package geumjeongyahak.domain.request.v1.dto.request;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;

public class AbsenceRequestPaginationRequest extends BasePaginationRequest {

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
