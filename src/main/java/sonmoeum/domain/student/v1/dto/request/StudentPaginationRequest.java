package sonmoeum.domain.student.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageRequest;
import sonmoeum.domain.base.dto.request.BasePaginationRequest;

@Schema(description = "학생 페이징 요청 DTO")
public class StudentPaginationRequest extends BasePaginationRequest {

    @Override
    public PageRequest toRequest() {
        return PageRequest.of(getPage(), getSize());
    }
}
