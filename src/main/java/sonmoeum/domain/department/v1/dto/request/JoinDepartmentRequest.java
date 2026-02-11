package sonmoeum.domain.department.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "부서 가입 요청")
public record JoinDepartmentRequest(

        @Schema(description = "부서 ID", example = "1")
        @NotNull(message = "부서 ID는 필수입니다.")
        Long departmentId
) {
}
