package sonmoeum.domain.student.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import sonmoeum.domain.base.dto.request.BasePaginationRequest;
import sonmoeum.domain.student.enums.StudentStatus;

@Schema(description = "학생 페이징 요청 DTO")
@Getter
@Setter
public class StudentPaginationRequest extends BasePaginationRequest {

    @Schema(description = "이름 검색(부분 일치)", example = "홍")
    private String name;

    @Schema(description = "학생 상태", example = "ENROLLED")
    private StudentStatus status;

    @Override
    public PageRequest toRequest() {
        return PageRequest.of(getPage(), getSize());
    }
}
