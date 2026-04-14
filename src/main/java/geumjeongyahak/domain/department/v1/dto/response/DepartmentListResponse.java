package geumjeongyahak.domain.department.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import geumjeongyahak.domain.department.entity.Department;

import java.util.List;

@Schema(description = "부서 목록 응답")
public record DepartmentListResponse (
    @Schema(description = "부서 목록")
    List<DepartmentResponse> departments
) {
    public static DepartmentListResponse from(List<Department> departments) {
        List<DepartmentResponse> departmentResponses = departments.stream()
                .map(DepartmentResponse::from)
                .toList();
        return new DepartmentListResponse(departmentResponses);
    }
}

