package geumjeongyahak.domain.department.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부서 수정 요청")
public record UpdateDepartmentRequest(
        @Schema(description = "부서 이름", example = "개발팀")
        String name,
        @Schema(description = "부서 설명", example = "소프트웨어 개발을 담당하는 팀")
        String description
) {

}
