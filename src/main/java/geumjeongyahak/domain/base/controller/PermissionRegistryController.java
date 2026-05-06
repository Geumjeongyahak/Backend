package geumjeongyahak.domain.base.controller;

import geumjeongyahak.domain.base.model.PermissionRegistry;
import geumjeongyahak.domain.base.model.PermissionRegistry.PermissionDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permission-registry")
@Tag(name = "Permission Registry", description = "관리자 권한 선택지 조회 API")
public class PermissionRegistryController {

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:manage:*') or hasAuthority('department:manage:*')")
    @GetMapping
    @Operation(summary = "전역 권한 선택지 조회", description = "관리자 화면에서 사용자/부서 권한을 선택할 때 사용할 수 있는 전역 권한 목록을 반환합니다.")
    public ResponseEntity<List<PermissionDefinition>> getGlobalPermissions() {
        return ResponseEntity.ok(PermissionRegistry.getGlobalPermissions());
    }
}
