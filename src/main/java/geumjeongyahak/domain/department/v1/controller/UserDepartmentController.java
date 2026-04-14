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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.department.service.DepartmentParticipateService;
import geumjeongyahak.domain.department.v1.dto.request.JoinDepartmentRequest;
import geumjeongyahak.domain.department.v1.dto.response.DepartmentListResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Department", description = "사용자 부서 참여 관리 API")
public class UserDepartmentController {
    private final DepartmentParticipateService departmentParticipateService;

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    @Operation(summary = "사용자 부서 참여", description = "사용자가 부서에 참여합니다.")
    @PostMapping("/{userId}/departments")
    public ResponseEntity<Void> joinDepartment(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,
            @Valid @RequestBody JoinDepartmentRequest request
    ) {
        log.debug("POST /api/v1/users/{}/departments - 부서 참여 요청: departmentId={}", userId, request.departmentId());
        departmentParticipateService.joinDepartment(userId, request.departmentId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    @Operation(summary = "사용자 부서 탈퇴", description = "사용자가 부서에서 탈퇴합니다.")
    @DeleteMapping("/{userId}/departments/{departmentId}")
    public ResponseEntity<Void> leaveDepartment(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,
            @Parameter(description = "부서 ID", example = "1")
            @PathVariable Long departmentId
    ) {
        log.debug("DELETE /api/v1/users/{}/departments/{} - 부서 탈퇴 요청", userId, departmentId);
        departmentParticipateService.leaveDepartment(userId, departmentId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "사용자 부서 목록 조회", description = "특정 사용자가 소속된 부서 목록을 조회합니다.")
    @GetMapping("/{userId}/departments")
    public ResponseEntity<DepartmentListResponse> getUserDepartments(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        log.debug("GET /api/v1/users/{}/departments - 사용자 소속 부서 목록 조회 요청", userId);
        DepartmentListResponse response = departmentParticipateService.getUserDepartments(userId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "본인 부서 목록 조회", description = "현재 로그인한 사용자가 소속된 부서 목록을 조회합니다.")
    @GetMapping("/me/departments")
    public ResponseEntity<DepartmentListResponse> getMyDepartments(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/users/me/departments - 본인 소속 부서 목록 조회 요청: userId={}", userDetails.getUserId());
        DepartmentListResponse response = departmentParticipateService.getUserDepartments(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
}
