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
@Tag(
    name = "Department",
    description = """
        부서 조회와 관리자용 부서 관리를 담당하는 API입니다.
        부서 권한은 department_permissions에 저장되며, MEMBER/MANAGER 직책별로 적용됩니다.
        """
)
public class DepartmentAdminController {

    private final DepartmentCrudService departmentCrudService;


    @PreAuthorize("""
        hasRole('ADMIN') or (
            hasAuthority('department:write:*') and
            (#request.permissions() == null or #request.permissions().isEmpty() or hasAuthority('department:grant:*'))
        )
        """)
    @Operation(
        summary = "부서 생성",
        description = """
            새로운 부서를 생성합니다.

            동작 방식:
            - name, description으로 부서를 생성합니다.
            - permissions가 있으면 부서 직책별 권한으로 저장합니다.
            - permissions의 roleType을 생략하면 MEMBER 권한으로 저장합니다.
            - 부서 생성 후 부서 채널 생성 이벤트가 처리되면 해당 채널 read/write 기본 권한이 MEMBER에 추가될 수 있습니다.

            권한:
            - ADMIN은 모든 요청을 수행할 수 있습니다.
            - department:write:* 권한자는 권한 목록 없이 부서를 생성할 수 있습니다.
            - permissions를 함께 저장하려면 department:grant:* 권한도 필요합니다.
            """
    )
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
    @Operation(
        summary = "부서 수정",
        description = """
            부서 정보를 수정합니다.

            동작 방식:
            - name, description은 전달한 값만 변경합니다.
            - permissions가 null이면 기존 부서 권한을 유지합니다.
            - permissions를 보내면 부분 수정이 아니라 해당 부서의 기존 권한을 모두 삭제한 뒤 요청 목록으로 전체 교체합니다.
            - permissions의 roleType을 생략하면 MEMBER 권한으로 저장합니다.

            권한:
            - ADMIN은 모든 요청을 수행할 수 있습니다.
            - department:manage:* 권한자는 권한 목록 없이 부서 정보를 수정할 수 있습니다.
            - permissions를 함께 교체하려면 department:grant:* 권한도 필요합니다.
            """
    )
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
    @Operation(
        summary = "부서 삭제",
        description = """
            부서를 삭제합니다.

            동작 방식:
            - 소속 사용자가 있는 부서는 삭제할 수 없습니다.
            - 삭제되면 department_permissions의 해당 부서 권한도 cascade로 삭제됩니다.

            권한:
            - ADMIN 또는 department:manage:* 권한이 필요합니다.
            """
    )
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
