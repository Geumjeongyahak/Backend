package sonmoeum.api.v1.auth;

import sonmoeum.api.v1.auth.dto.request.EmailLoginRequest;
import sonmoeum.api.v1.common.dto.response.ApiResponse;
import sonmoeum.api.v1.users.dto.response.UserResponse;
import sonmoeum.common.security.jwt.JwtTokenProvider;
import sonmoeum.common.security.service.CustomUserDetails;
import sonmoeum.domain.users.service.UserCrudService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserCrudService userCrudService;

    // ✅ 너가 만든 JwtTokenProvider 사용 (패키지명은 프로젝트에 맞춰)
    private final JwtTokenProvider jwtTokenProvider;

    // ✅ SecurityProperties에서 refresh 쿠키 옵션을 가져오는 방식이 가장 좋지만,
    // 여기서는 최소 예시로 상수로 둠(프로젝트에 맞게 교체 추천)
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    public record LoginResponse(
        String accessToken,
        UserResponse user
    ) {}

    public record RefreshResponse(String accessToken) {}

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인 후 JWT를 발급합니다.")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody EmailLoginRequest request,
        jakarta.servlet.http.HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // ✅ principal이 CustomUserDetails라고 가정(기존 코드도 그렇게 사용 중) :contentReference[oaicite:2]{index=2}
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserResponse userResponse = userCrudService.getUserById(userDetails.getUserId());

        String accessToken = jwtTokenProvider.createAccessToken(
            userDetails.getUsername(), // 보통 email/username
            userDetails.getAuthorities()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(userDetails.getUsername());

        // ✅ refresh 토큰은 HttpOnly 쿠키로 내려줌
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
            .httpOnly(true)
            .secure(false) // dev에서는 false, prod에서는 true 권장
            .path("/")
            .sameSite("Lax")
            .maxAge(60L * 60 * 24 * 14) // 14일 예시 (프로퍼티로 빼는 걸 추천)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ApiResponse.success(new LoginResponse(accessToken, userResponse));
    }

    @Operation(summary = "토큰 재발급", description = "refresh 쿠키로 access 토큰을 재발급합니다.")
    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(
        @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
        jakarta.servlet.http.HttpServletResponse response
    ) {
        if (!StringUtils.hasText(refreshToken) || !jwtTokenProvider.validate(refreshToken)) {
            // 프로젝트 공통 예외/에러 포맷에 맞게 바꿔도 됨
            throw new IllegalStateException("유효한 refresh 토큰이 없습니다.");
        }

        String subject = jwtTokenProvider.getSubject(refreshToken);

        // 여기서 refresh rotate(회전)를 할지 여부는 다음 단계에서 결정
        String newAccessToken = jwtTokenProvider.createAccessToken(subject, java.util.List.of());

        return ApiResponse.success(new RefreshResponse(newAccessToken));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "로그아웃", description = "refresh 쿠키를 삭제합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(jakarta.servlet.http.HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .sameSite("Lax")
            .maxAge(0)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        return ApiResponse.success(null);
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