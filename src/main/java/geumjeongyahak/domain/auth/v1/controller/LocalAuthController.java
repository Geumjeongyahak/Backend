package geumjeongyahak.domain.auth.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.mail.MailProperties;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.auth.exception.AuthErrorCode;
import geumjeongyahak.domain.auth.service.EmailVerificationService;
import geumjeongyahak.domain.auth.service.LocalAuthService;
import geumjeongyahak.domain.auth.service.PasswordResetService;
import geumjeongyahak.domain.auth.v1.dto.request.EmailVerificationConfirmRequest;
import geumjeongyahak.domain.auth.v1.dto.request.EmailVerificationResendRequest;
import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.domain.auth.v1.dto.request.LocalSignupRequest;
import geumjeongyahak.domain.auth.v1.dto.request.LogoutRequest;
import geumjeongyahak.domain.auth.v1.dto.request.PasswordResetConfirmRequest;
import geumjeongyahak.domain.auth.v1.dto.request.PasswordResetRequest;
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
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final MailProperties mailProperties;

    @Operation(
        summary = "로그인",
        description = "이메일과 비밀번호로 로그인합니다. 비활성화되었거나 이메일 인증을 완료하지 않은 Local 사용자는 로그인할 수 없습니다."
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
        description = "관리자 페이지에서 사용할 ADMIN/MANAGER 전용 JWT 로그인을 수행합니다. 비활성화되었거나 이메일 인증을 완료하지 않은 Local 사용자는 로그인할 수 없습니다."
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
        description = "이메일, 비밀번호, 기본 정보와 생년월일로 새로운 사용자를 등록하고 이메일 인증번호를 발송합니다. 가입 직후 토큰은 발급하지 않습니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<AuthMessageResponse> signup(
            @Valid @RequestBody LocalSignupRequest request
    ) {
        log.debug("POST /api/v1/auth/signup - 회원가입 요청: {}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(localLoginService.signup(request));
    }

    @Operation(
        summary = "이메일 인증 확정",
        description = "회원가입 또는 재발송 메일로 받은 6자리 인증번호로 Local 로그인 이메일을 인증합니다."
    )
    @PostMapping("/email-verification/confirm")
    public ResponseEntity<AuthMessageResponse> confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        log.debug("POST /api/v1/auth/email-verification/confirm - 이메일 인증 요청");
        emailVerificationService.confirm(request.email(), request.verificationCode());
        return ResponseEntity.ok(AuthMessageResponse.of("이메일 인증이 완료되었습니다. 로그인해 주세요."));
    }

    @Operation(
        summary = "이메일 인증 링크 처리",
        description = "메일의 인증 버튼에서 호출되며, 인증 결과를 프론트엔드 인증 결과 페이지로 리다이렉트합니다."
    )
    @GetMapping("/email-verification/confirm")
    public void confirmEmailVerificationLink(
            @RequestParam String email,
            @RequestParam("code") String verificationCode,
            HttpServletResponse response
    ) throws IOException {
        String status = "success";
        try {
            emailVerificationService.confirm(email, verificationCode);
        } catch (BusinessException exception) {
            status = AuthErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED.getCode().equals(exception.getCode())
                ? "expired"
                : "invalid";
            log.info("이메일 인증 링크 처리 실패 - email={}, status={}", email, status);
        }

        response.sendRedirect(UriComponentsBuilder
            .fromUriString(mailProperties.frontendBaseUrl())
            .replacePath("/auth/email-verification")
            .queryParam("status", status)
            .queryParam("email", email)
            .toUriString());
    }

    @Operation(
        summary = "이메일 인증 메일 재발송",
        description = "미인증 Local 계정에 이메일 인증번호를 다시 발송합니다. 계정 존재 여부는 노출하지 않고 항상 동일한 성공 응답을 반환합니다."
    )
    @PostMapping("/email-verification/resend")
    public ResponseEntity<AuthMessageResponse> resendEmailVerification(
            @Valid @RequestBody EmailVerificationResendRequest request
    ) {
        log.debug("POST /api/v1/auth/email-verification/resend - 이메일 인증 재발송 요청: {}", request.email());
        emailVerificationService.resend(request.email());
        return ResponseEntity.ok(AuthMessageResponse.of("인증 메일이 발송되었습니다. 메일이 도착하지 않으면 입력한 이메일을 확인해 주세요."));
    }

    @Operation(
        summary = "비밀번호 재설정 메일 요청",
        description = "비밀번호를 모르는 사용자가 Local 로그인 이메일로 6자리 인증번호를 요청합니다. 계정 존재 여부는 노출하지 않고 항상 동일한 성공 응답을 반환합니다."
    )
    @PostMapping("/password-reset/request")
    public ResponseEntity<AuthMessageResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        log.debug("POST /api/v1/auth/password-reset/request - 비밀번호 재설정 요청: {}", request.email());
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(AuthMessageResponse.of("비밀번호 재설정 안내 메일이 발송되었습니다. 메일이 도착하지 않으면 입력한 이메일을 확인해 주세요."));
    }

    @Operation(
        summary = "비밀번호 재설정 확정",
        description = "메일로 받은 6자리 인증번호와 새 비밀번호로 Local 로그인 비밀번호를 재설정합니다."
    )
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<AuthMessageResponse> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        log.debug("POST /api/v1/auth/password-reset/confirm - 비밀번호 재설정 확정 요청");
        passwordResetService.resetPassword(request.email(), request.resetCode(), request.newPassword());
        return ResponseEntity.ok(AuthMessageResponse.of("비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해 주세요."));
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
