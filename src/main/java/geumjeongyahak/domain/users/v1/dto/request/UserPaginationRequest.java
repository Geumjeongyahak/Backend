package geumjeongyahak.domain.users.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageRequest;
import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;

@Schema(description = "사용자 목록 페이징 요청 DTO. page와 size를 이용해 사용자 목록 조회 범위를 지정합니다.")
public class UserPaginationRequest extends BasePaginationRequest {

    @Override
    public PageRequest toRequest() {
       return PageRequest.of(getPage(), getSize());
    }
}
