package geumjeongyahak.domain.users.v1.controller;

import geumjeongyahak.domain.auth.v1.dto.response.RoleResponse;
import geumjeongyahak.domain.users.service.UserPermissionService;
import geumjeongyahak.domain.users.v1.dto.request.UserPermissionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}/permissions")
@RequiredArgsConstructor
@Tag(name = "User Permission", description = "사용자 세부 권한 관리 API")
@PreAuthorize("hasRole('ADMIN')")
public class UserPermissionController {
    private final UserPermissionService userPermissionService;

    @Operation(summary = "사용자 권한 목록 조회", description = "특정 사용자의 세부 권한을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getUserPermissions(@PathVariable Long userId) {
        log.debug("GET /api/v1/users/{}/permissions - 사용자 권한 목록 조회 요청", userId);
        return ResponseEntity.ok(userPermissionService.getAllPermissions(userId));
    }

    @Operation(summary = "사용자 권한 추가", description = "사용자에게 세부 권한을 추가합니다.")
    @PostMapping
    public ResponseEntity<List<RoleResponse>> addPermission(
        @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId,
        @Valid @RequestBody UserPermissionRequest request
    ) {
        return ResponseEntity.ok(userPermissionService.addPermission(
            userId,
            request.permissionCode()
        ));
    }

    @Operation(summary = "사용자 권한 제거", description = "사용자에게서 세부 권한을 제거합니다.")
    @DeleteMapping
    public ResponseEntity<List<RoleResponse>> removePermission(
        @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId,
        @Valid @RequestBody UserPermissionRequest request
    ) {
        return ResponseEntity.ok(userPermissionService.removePermission(
            userId,
            request.permissionCode()
        ));
    }
}
