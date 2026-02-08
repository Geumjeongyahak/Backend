package sonmoeum.api.v1.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sonmoeum.api.v1.auth.dto.request.EmailLoginRequest;
import sonmoeum.api.v1.auth.dto.request.EmailSignupRequest;
import sonmoeum.api.v1.auth.dto.response.LoginResponse;
import sonmoeum.api.v1.common.dto.response.ApiResponse;
import sonmoeum.api.v1.users.dto.response.UserResponse;
import sonmoeum.common.security.jwt.JwtTokenProvider;
import sonmoeum.common.security.service.CustomUserDetails;
import sonmoeum.domain.auth.service.EmailAuthService;
import sonmoeum.domain.users.service.UserCrudService;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserCrudService userCrudService;
    private final EmailAuthService emailAuthService;

    @Operation(summary = "회원가입", description = "이메일 정보로 새로운 사용자를 등록합니다.")
    @PostMapping("/signup")
    public ApiResponse<UserResponse> signUp(@Valid @RequestBody EmailSignupRequest request) {
        UserResponse response = emailAuthService.signUp(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인 후 JWT를 발급합니다.")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody EmailLoginRequest request) {
        LoginResponse response = emailAuthService.login(request);
        return ApiResponse.success(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        UserResponse userResponse = userCrudService.getUserById(userDetails.getUserId());
        return ApiResponse.success(userResponse);
    }
}