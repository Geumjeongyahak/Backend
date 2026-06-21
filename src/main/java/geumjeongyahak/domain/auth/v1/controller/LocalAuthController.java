package geumjeongyahak.domain.auth.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.auth.service.LocalAuthService;
import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.domain.auth.v1.dto.request.LocalSignupRequest;
import geumjeongyahak.domain.auth.v1.dto.request.LogoutRequest;
import geumjeongyahak.domain.auth.v1.dto.request.RefreshTokenRequest;
import geumjeongyahak.domain.auth.v1.dto.response.AuthMessageResponse;
import geumjeongyahak.domain.auth.v1.dto.response.TokenResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class LocalAuthController {
    private final LocalAuthService localLoginService;

    @Operation(
        summary = "로그인",
        description = "이메일과 비밀번호로 로그인합니다. 비활성화된 사용자는 로그인할 수 없습니다."
    )
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LocalLoginRequest request
    ) {
        log.debug("POST /api/v1/auth/login - 로그인 요청: {}", request.email());
        return ResponseEntity.ok(localLoginService.login(request));
    }

    @Operation(
        summary = "관리자 로그인",
        description = "관리자 페이지에서 사용할 ADMIN/MANAGER 전용 JWT 로그인을 수행합니다. 비활성화된 사용자는 로그인할 수 없습니다."
    )
    @PostMapping("/admin/login")
    public ResponseEntity<TokenResponse> adminLogin(
            @Valid @RequestBody LocalLoginRequest request
    ) {
        log.debug("POST /api/v1/auth/admin/login - 관리자 로그인 요청: {}", request.email());
        return ResponseEntity.ok(localLoginService.adminLogin(request));
    }

    @Operation(
        summary = "회원가입",
        description = "이메일, 비밀번호, 기본 정보와 생년월일로 새로운 사용자를 등록합니다. 생년월일은 내부 저장 형식으로 변환되며, 프로필 이미지는 가입 후 별도 업로드 API로 등록합니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(
            @Valid @RequestBody LocalSignupRequest request
    ) {
        log.debug("POST /api/v1/auth/signup - 회원가입 요청: {}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(localLoginService.signup(request));
    }

    @Operation(
        summary = "토큰 재발급",
        description = "Refresh Token으로 새로운 Access Token을 발급받습니다. 비활성 사용자이거나 폐기된 Refresh Token이면 재발급되지 않습니다."
    )
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        log.debug("POST /api/v1/auth/refresh - 토큰 재발급 요청");
        TokenResponse response = localLoginService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "Refresh Token을 무효화하여 로그아웃합니다.")
    @PostMapping("/logout")
    public ResponseEntity<AuthMessageResponse> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        log.debug("POST /api/v1/auth/logout - 로그아웃 요청");
        localLoginService.logout(request.refreshToken());
        return ResponseEntity.ok(AuthMessageResponse.of("로그아웃되었습니다."));
    }

    @Operation(summary = "전체 디바이스 로그아웃", description = "현재 사용자의 모든 디바이스에서 로그아웃합니다.")
    @PostMapping("/logout-all")
    public ResponseEntity<AuthMessageResponse> logoutAllDevices(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/auth/me/logout-all - 전체 디바이스 로그아웃 요청: userId={}", userDetails.getUserId());
        localLoginService.logoutAllDevices(userDetails.getUserId());
        return ResponseEntity.ok(AuthMessageResponse.of("모든 디바이스에서 로그아웃되었습니다."));
    }
}
