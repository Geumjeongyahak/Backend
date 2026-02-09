package sonmoeum.domain.users.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.auth.v1.dto.response.RoleResponse;
import sonmoeum.domain.users.service.UserRoleService;
import sonmoeum.domain.users.v1.dto.request.AddSubRoleRequest;
import sonmoeum.domain.users.v1.dto.request.RemoveSubRoleRequest;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}/roles")
@RequiredArgsConstructor
@Tag(name = "User Sub-Role", description = "사용자 하위권한 관리 API (부서권한, 부가권한 등)")
public class UserRoleController {
    private final UserRoleService userRoleService;

    @Operation(summary = "사용자 역할 목록 조회", description = "특정 사용자의 모든 역할(하위권한 포함)을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getUserRoles(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        log.debug("GET /api/v1/users/{}/roles - 사용자 역할 목록 조회 요청", userId);
        List<RoleResponse> response = userRoleService.getAllRoles(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "사용자 하위권한 추가", description = "사용자에게 하위권한(부서권한, 부가권한 등)을 추가합니다. 기본 역할(ADMIN, MANAGER 등)은 추가할 수 없습니다.")
    @PostMapping
    public ResponseEntity<List<RoleResponse>> addSubRole(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,
            @Valid @RequestBody AddSubRoleRequest request
    ) {
        log.debug("POST /api/v1/users/{}/roles - 사용자 하위권한 추가 요청: {}", userId, request.subRole());
        RoleType roleType = RoleType.valueOf(request.subRole());
        List<RoleResponse> response = userRoleService.addRole(userId, roleType);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "사용자 하위권한 제거", description = "사용자에게서 하위권한(부서권한, 부가권한 등)을 제거합니다. 기본 역할(ADMIN, MANAGER 등)은 제거할 수 없습니다.")
    @DeleteMapping
    public ResponseEntity<List<RoleResponse>> removeSubRole(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,
            @Valid @RequestBody RemoveSubRoleRequest request
    ) {
        log.debug("DELETE /api/v1/users/{}/roles - 사용자 하위권한 제거 요청: {}", userId, request.subRole());
        RoleType roleType = RoleType.valueOf(request.subRole());
        List<RoleResponse> response = userRoleService.removeRole(userId, roleType);
        return ResponseEntity.ok(response);
    }
}
