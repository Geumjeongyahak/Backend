package geumjeongyahak.domain.department.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.department.entity.Department;

@Schema(description = "부서 기본 정보")
public record DepartmentSimpleResponse(
        @Schema(description = "부서 ID", example = "1")
        Long id,

        @Schema(description = "부서 이름", example = "개발팀")
        String name,

        @Schema(description = "부서 설명", example = "소프트웨어 개발을 담당하는 팀")
        String description
) {
    public static DepartmentSimpleResponse from(Department department) {
        return new DepartmentSimpleResponse(
                department.getId(),
                department.getName(),
                department.getDescription()
        );
    }
}
