package geumjeongyahak.domain.users.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageRequest;
import geumjeongyahak.domain.base.dto.request.BasePaginationRequest;

@Schema(description = "사용자 페이징 요청 DTO")
public class UserPaginationRequest extends BasePaginationRequest {

    @Override
    public PageRequest toRequest() {
       return PageRequest.of(getPage(), getSize());
    }
    /*
      * filter, sort 등의 추가 필드가 필요한 경우 추가
     */
}
