package sonmoeum.domain.users.v1.dto.request;

import org.springframework.data.domain.PageRequest;
import sonmoeum.domain.base.dto.request.BasePaginationRequest;

public class UserPaginationRequest extends BasePaginationRequest {

    @Override
    public PageRequest toRequest() {
       return PageRequest.of(getPage(), getSize());
    }
    /*
      * filter, sort 등의 추가 필드가 필요한 경우 추가
     */
}
