package sonmoeum.domain.department.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import sonmoeum.domain.department.entity.Department;

public record DepartmentResponse(
        @Schema(description = "부서 ID", example = "1")
        Long id,

        @Schema(description = "부서 이름", example = "개발팀")
        String name,

        @Schema(description = "부서 설명", example = "소프트웨어 개발을 담당하는 팀")
        String description
) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getDescription()
        );
    }
}
