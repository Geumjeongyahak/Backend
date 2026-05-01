package geumjeongyahak.domain.users.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.users.service.UserCrudService;
import geumjeongyahak.domain.users.v1.dto.request.CreateUserRequest;
import geumjeongyahak.domain.users.v1.dto.request.UpdateSelfRequest;
import geumjeongyahak.domain.users.v1.dto.request.UpdateUserRequest;
import geumjeongyahak.domain.users.v1.dto.request.UserPaginationRequest;
import geumjeongyahak.domain.users.v1.dto.response.UserSimpleResponse;
import geumjeongyahak.domain.users.v1.dto.response.UserDetailResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관리 API")
public class UserController {
    private final UserCrudService userCrudService;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "사용자 목록 조회", description = "전체 사용자 목록을 페이지네이션하여 조회합니다.")
    @GetMapping
    public ResponseEntity<PaginationResponse<UserSimpleResponse>> getAllUsers(
            @ParameterObject @Valid UserPaginationRequest request
    ) {
        log.debug("GET /api/v1/users - 사용자 목록 조회 요청");
        PaginationResponse<UserSimpleResponse> response = userCrudService.getAllUsersPagination(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "사용자 상세 조회", description = "ID로 특정 사용자를 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailResponse> getUserById(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        log.debug("GET /api/v1/users/{} - 사용자 상세 조회 요청", userId);
        UserDetailResponse response = userCrudService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "사용자 본인 조회", description = "현재 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<UserDetailResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/users/me - 현재 사용자 정보 조회 요청");
        UserDetailResponse response = userCrudService.getUserById(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "사용자 생성", description = "새로운 사용자를 생성합니다.")
    @PostMapping
    public ResponseEntity<UserDetailResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        log.debug("POST /api/v1/users - 사용자 생성 요청: {}", request.email());
        UserDetailResponse response = userCrudService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "사용자 수정", description = "기존 사용자 정보를 수정합니다.")
    @PatchMapping("/{userId}")
    public ResponseEntity<UserDetailResponse> updateUser(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        log.debug("PATCH /api/v1/users/{} - 사용자 수정 요청", userId);
        UserDetailResponse response = userCrudService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "사용자 본인 수정", description = "현재 사용자의 정보를 수정합니다.")
    @PatchMapping("/me")
    public ResponseEntity<UserDetailResponse> updateCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateSelfRequest request
    ) {
        log.debug("PATCH /api/v1/users/me - 현재 사용자 정보 수정 요청");
        UserDetailResponse response = userCrudService.updateUser(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "사용자 삭제", description = "사용자를 삭제합니다.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        log.debug("DELETE /api/v1/users/{} - 사용자 삭제 요청", userId);
        userCrudService.deleteUserById(userId);
        return ResponseEntity.noContent().build();
    }
}
