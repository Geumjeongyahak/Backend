package sonmoeum.api.v1.departments.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import sonmoeum.domain.auth.enums.PermissionType;
import sonmoeum.domain.department.entity.Department;

import io.swagger.v3.oas.annotations.media.Schema;

public record DepartmentResponse(
    @Schema(description = "부서 ID", example = "1")
    Long id,
    @Schema(description = "부서 이름", example = "교무부")
    String name,
    @Schema(description = "부서 설명", example = "교무 관련 업무 수행")
    String description,
    @Schema(description = "부서 권한 목록", example = "[\"MANAGE_LESSONS\"]")
    List<String> permissions
) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
            department.getId(),
            department.getName(),
            department.getDescription(),
            department.getPermissions().stream()
                .map(permission -> PermissionType.findById(permission.getPermissionId())
                    .map(Enum::name)
                    .orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList())
        );
    }
}
