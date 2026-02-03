package sonmoeum.api.v1.departments;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.ApiResponse;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.departments.dto.request.CreateDepartmentRequest;
import sonmoeum.api.v1.departments.dto.request.UpdateDepartmentRequest;
import sonmoeum.api.v1.departments.dto.response.DepartmentResponse;
import sonmoeum.domain.department.service.DepartmentCrudService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Departments", description = "부서 관리 API")
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentCrudService departmentCrudService;

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_DEPARTMENTS')")
    @Operation(summary = "부서 목록 조회", description = "참여 가능한 부서 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<BasePageResponse<DepartmentResponse>> getDepartments(BasePageRequest pageRequest) {
        return ApiResponse.success(departmentCrudService.getDepartmentPagination(pageRequest));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_DEPARTMENTS')")
    @Operation(summary = "부서 상세 조회", description = "ID로 부서를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<DepartmentResponse> getDepartment(@PathVariable Long id) {
        return ApiResponse.success(departmentCrudService.getDepartmentById(id));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_DEPARTMENTS')")
    @Operation(summary = "부서 생성", description = "새로운 부서를 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DepartmentResponse> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return ApiResponse.success(departmentCrudService.createDepartment(request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_DEPARTMENTS')")
    @Operation(summary = "부서 수정", description = "부서 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return ApiResponse.success(departmentCrudService.updateDepartment(id, request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_DEPARTMENTS')")
    @Operation(summary = "부서 삭제", description = "부서를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDepartment(@PathVariable Long id) {
        departmentCrudService.deleteDepartment(id);
        return ApiResponse.success(null);
    }
}
