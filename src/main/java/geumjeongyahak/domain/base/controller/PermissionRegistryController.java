package geumjeongyahak.domain.base.controller;

import java.util.List;

import geumjeongyahak.domain.base.model.PermissionDefinition;
import geumjeongyahak.domain.base.service.PermissionRegistryViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permission-registry")
@Tag(name = "Permission Registry", description = "사용자 직접 권한과 부서 직책별 권한에 부여 가능한 permission code 선택지 조회 API")
@RequiredArgsConstructor
public class PermissionRegistryController {

    private final PermissionRegistryViewService permissionRegistryViewService;

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:grant:*') or hasAuthority('department:grant:*')")
    @GetMapping
    @Operation(
        summary = "권한 선택지 조회",
        description = """
            관리자 화면에서 사용자 직접 권한 또는 부서 직책별 권한을 선택할 때 사용할 수 있는 permission code 목록을 반환합니다.
            사용자 권한 부여 화면에서는 user_permissions에 저장할 MANUAL 권한 선택지로 사용하고,
            부서 권한 관리 화면에서는 department_permissions에 저장할 MEMBER/MANAGER 권한 선택지로 사용합니다.
            """
    )
    public ResponseEntity<List<PermissionDefinition>> getAssignablePermissions() {
        return ResponseEntity.ok(permissionRegistryViewService.getAssignablePermissions());
    }
}
