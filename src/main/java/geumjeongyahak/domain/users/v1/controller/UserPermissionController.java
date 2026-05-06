package geumjeongyahak.domain.users.v1.controller;

import geumjeongyahak.domain.auth.v1.dto.response.PermissionResponse;
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
@Tag(
    name = "User Permission",
    description = """
        사용자에게 직접 부여되는 세부 권한(authority)을 조회, 추가, 제거하는 API입니다.
        기본 역할(role)과 별개로 예외적인 운영 권한을 부여해야 할 때 사용합니다.
        """
)
public class UserPermissionController {
    private final UserPermissionService userPermissionService;

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:read:*')")
    @Operation(
        summary = "사용자 권한 목록 조회",
        description = """
            특정 사용자에게 직접 부여된 세부 권한 목록을 조회합니다.

            사용 사례:
            - 운영 권한 부여 전 현재 권한 상태 확인
            - 사용자 상세 화면에서 authority 목록 표시

            동작 방식:
            - 역할(role) 기반 권한이 아니라 사용자에 직접 저장된 permission code만 반환합니다.
            - 부서 권한이나 role 기반 권한은 이 API 응답에 포함되지 않습니다.

            사이드 이펙트:
            - 읽기 전용 API이며 권한 상태를 변경하지 않습니다.
            """
    )
    @GetMapping
    public ResponseEntity<List<PermissionResponse>> getUserPermissions(@PathVariable Long userId) {
        log.debug("GET /api/v1/users/{}/permissions - 사용자 권한 목록 조회 요청", userId);
        return ResponseEntity.ok(userPermissionService.getAllPermissions(userId));
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:manage:*')")
    @Operation(
        summary = "사용자 권한 추가",
        description = """
            특정 사용자에게 세부 권한 하나를 직접 추가합니다.

            사용 사례:
            - 특정 사용자에게 운영용 authority 예외 부여
            - role만으로 표현하기 어려운 세부 권한 보완

            동작 방식:
            - 요청한 permissionCode가 이미 있으면 중복 저장하지 않습니다.
            - 권한 추가 후 최종 권한 목록 전체를 다시 반환합니다.

            사이드 이펙트:
            - user_permissions 테이블에 권한이 추가될 수 있습니다.
            - 이후 해당 사용자의 인증/인가 결과에 직접 영향을 줄 수 있습니다.
            """
    )
    @PostMapping
    public ResponseEntity<List<PermissionResponse>> addPermission(
        @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId,
        @Valid @RequestBody UserPermissionRequest request
    ) {
        return ResponseEntity.ok(userPermissionService.addPermission(
            userId,
            request.permissionCode()
        ));
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:manage:*')")
    @Operation(
        summary = "사용자 권한 제거",
        description = """
            특정 사용자에게 직접 부여된 세부 권한 하나를 제거합니다.

            사용 사례:
            - 임시 운영 권한 회수
            - 잘못 부여된 authority 정리

            동작 방식:
            - 요청한 permissionCode가 존재할 때만 제거합니다.
            - 권한 제거 후 최종 권한 목록 전체를 다시 반환합니다.

            사이드 이펙트:
            - user_permissions 테이블에서 해당 권한이 삭제될 수 있습니다.
            - 이후 해당 사용자의 인증/인가 결과에 직접 영향을 줄 수 있습니다.
            """
    )
    @DeleteMapping
    public ResponseEntity<List<PermissionResponse>> removePermission(
        @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId,
        @Valid @RequestBody UserPermissionRequest request
    ) {
        return ResponseEntity.ok(userPermissionService.removePermission(
            userId,
            request.permissionCode()
        ));
    }
}
