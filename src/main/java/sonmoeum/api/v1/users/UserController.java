package sonmoeum.api.v1.users;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.ApiResponse;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.users.dto.request.CreateEmailUserRequest;
import sonmoeum.api.v1.users.dto.request.UpdateUserRequest;
import sonmoeum.api.v1.users.dto.response.UserResponse;
import sonmoeum.domain.users.service.UserCrudService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Users", description = "사용자 관리 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCrudService userCrudService;

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_USERS')")
    @Operation(summary = "사용자 목록 조회", description = "페이지네이션된 사용자 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<BasePageResponse<UserResponse>> getUsers(BasePageRequest pageRequest) {
        return ApiResponse.success(userCrudService.getUserPagination(pageRequest));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_USERS')")
    @Operation(summary = "사용자 상세 조회", description = "ID로 사용자를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ApiResponse.success(userCrudService.getUserById(id));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_USERS')")
    @Operation(summary = "사용자 생성", description = "이메일 기반 사용자를 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateEmailUserRequest request) {
        return ApiResponse.success(userCrudService.createUser(request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_USERS')")
    @Operation(summary = "사용자 수정", description = "사용자 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userCrudService.updateUser(id, request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_USERS')")
    @Operation(summary = "사용자 삭제", description = "사용자를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userCrudService.deleteUser(id);
        return ApiResponse.success(null);
    }
}
