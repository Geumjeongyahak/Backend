package geumjeongyahak.domain.department.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.domain.department.service.DepartmentCrudService;
import geumjeongyahak.domain.department.v1.dto.request.CreateDepartmentRequest;
import geumjeongyahak.domain.department.v1.dto.request.UpdateDepartmentRequest;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentDetailResponse;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentListResponse;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Department", description = "부서 관리 API")
public class DepartmentController {
    private final DepartmentCrudService departmentCrudService;

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "부서 목록 조회", description = "참여 가능한 부서 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<DepartmentListResponse> getAllDepartments() {
        log.debug("GET /api/v1/departments - 부서 목록 조회 요청");
        DepartmentListResponse response = departmentCrudService.getAllDepartments();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "부서 상세 조회", description = "ID로 부서를 조회합니다. 할당된 역할과 소속 사용자 목록을 포함합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDetailResponse> getDepartmentById(
            @Parameter(description = "부서 ID", example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/departments/{} - 부서 상세 조회 요청", id);
        DepartmentDetailResponse response = departmentCrudService.getDepartmentDetailById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "부서 생성", description = "새로운 부서를 생성합니다.")
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request
    ) {
        log.debug("POST /api/v1/departments - 부서 생성 요청: {}", request.name());
        DepartmentResponse response = departmentCrudService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "부서 수정", description = "부서 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @Parameter(description = "부서 ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        log.debug("PUT /api/v1/departments/{} - 부서 수정 요청", id);
        DepartmentResponse response = departmentCrudService.updateDepartment(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "부서 삭제", description = "부서를 삭제합니다. 할당된 역할이나 멤버가 있으면 삭제할 수 없습니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(
            @Parameter(description = "부서 ID", example = "1")
            @PathVariable Long id
    ) {
        log.debug("DELETE /api/v1/departments/{} - 부서 삭제 요청", id);
        departmentCrudService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
