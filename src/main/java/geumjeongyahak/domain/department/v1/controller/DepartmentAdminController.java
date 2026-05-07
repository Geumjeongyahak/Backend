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
import geumjeongyahak.domain.department.v1.dto.response.DepartmentSimpleResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Department", description = "부서 관리 API")
public class DepartmentAdminController {

    private final DepartmentCrudService departmentCrudService;


    @PreAuthorize("""
        hasRole('ADMIN') or (
            hasAuthority('department:write:*') and
            (#request.permissions() == null or #request.permissions().isEmpty() or hasAuthority('department:grant:*'))
        )
        """)
    @Operation(summary = "부서 생성", description = "새로운 부서를 생성합니다.")
    @PostMapping
    public ResponseEntity<DepartmentSimpleResponse> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request
    ) {
        log.debug("POST /api/v1/departments - 부서 생성 요청: {}", request.name());
        DepartmentSimpleResponse response = departmentCrudService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("""
        hasRole('ADMIN') or (
            hasAuthority('department:manage:*') and
            (#request.permissions() == null or #request.permissions().isEmpty() or hasAuthority('department:grant:*'))
        )
        """)
    @Operation(summary = "부서 수정", description = "부서 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentSimpleResponse> updateDepartment(
            @Parameter(description = "부서 ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        log.debug("PUT /api/v1/departments/{} - 부서 수정 요청", id);
        DepartmentSimpleResponse response = departmentCrudService.updateDepartment(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('department:manage:*')")
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
