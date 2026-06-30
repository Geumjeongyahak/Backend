package geumjeongyahak.domain.department.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.domain.department.service.DepartmentCrudService;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentDetailResponse;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentListResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(
    name = "Department",
    description = "부서 조회 API입니다. 상세 조회 응답에는 부서 직책별 권한과 소속 사용자 목록이 포함됩니다."
)
public class DepartmentController {
    private final DepartmentCrudService departmentCrudService;

    @Operation(summary = "부서 목록 조회", description = "참여 가능한 부서 목록을 조회합니다. 부서 권한 목록은 상세 조회에서 확인합니다.")
    @GetMapping
    public ResponseEntity<DepartmentListResponse> getAllDepartments() {
        log.debug("GET /api/v1/departments - 부서 목록 조회 요청");
        DepartmentListResponse response = departmentCrudService.getAllDepartments();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "부서 상세 조회", description = "ID로 부서를 조회합니다. department_permissions에 저장된 MEMBER/MANAGER 직책별 권한과 소속 사용자 목록을 포함합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDetailResponse> getDepartmentById(
            @Parameter(description = "부서 ID", example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/departments/{} - 부서 상세 조회 요청", id);
        DepartmentDetailResponse response = departmentCrudService.getDepartmentDetailById(id);
        return ResponseEntity.ok(response);
    }
}
