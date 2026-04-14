package geumjeongyahak.domain.department.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.v1.dto.response.RoleResponse;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.entity.UserDepartment;
import geumjeongyahak.domain.users.v1.dto.response.UserResponse;

import java.util.Collection;
import java.util.List;

@Schema(description = "부서 상세 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DepartmentDetailResponse (
        @Schema(description = "부서 ID", example = "1")
        Long id,

        @Schema(description = "부서 이름", example = "개발팀")
        String name,

        @Schema(description = "부서 설명", example = "소프트웨어 개발을 담당하는 팀")
        String description,

        @Schema(description = "할당된 역할 정보")
        RoleResponse assignedRole,

        @Schema(description = "부서에 속한 사용자 목록")
        List<UserResponse> users
) {
        public static DepartmentDetailResponse from(Department department, Collection<UserDepartment> userDepartments) {
            return new DepartmentDetailResponse(
                    department.getId(),
                    department.getName(),
                    department.getDescription(),
                    (department.getRoleId() != null) ? RoleResponse.from(RoleType.findById(department.getRoleId())) : null,
                    userDepartments.stream()
                            .map(ud -> UserResponse.from(ud.getUser()))
                            .toList()
            );
        }
}
