package geumjeongyahak.domain.auth.v1.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import geumjeongyahak.common.config.GoogleOAuth2Properties;
import geumjeongyahak.domain.auth.service.GoogleAuthService;
import geumjeongyahak.domain.auth.v1.dto.request.GoogleLoginRequest;
import geumjeongyahak.domain.auth.v1.dto.request.GoogleSignupRequest;
import geumjeongyahak.domain.auth.v1.dto.response.GoogleCallbackResponse;
import geumjeongyahak.domain.auth.v1.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/google")
@RequiredArgsConstructor
@Tag(name = "Google Auth", description = "Google OAuth2 인증 API")
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;
    private final GoogleOAuth2Properties googleProperties;

    @Operation(summary = "Google 로그인 시작", description = "Google OAuth2 인증 페이지로 리다이렉트합니다.")
    @GetMapping
    public void redirectToGoogle(HttpServletResponse response) throws IOException {
        response.sendRedirect(googleAuthService.getAuthorizationUrl());
    }

    @Operation(summary = "Google OAuth2 콜백", description = "Google 인증 후 tempToken과 signupRequired를 담아 프론트엔드로 리다이렉트합니다.")
    @GetMapping("/callback")
    public void handleCallback(
            @RequestParam String code,
            HttpServletResponse response
    ) throws IOException {
        try {
            GoogleCallbackResponse result = googleAuthService.handleCallback(code);
            String redirectUrl = UriComponentsBuilder
                .fromUriString(googleProperties.getFrontendRedirectUri())
                .queryParam("tempToken", result.tempToken())
                .queryParam("signupRequired", result.signupRequired())
                .queryParam("connectedToLocal", result.connectedToLocal())
                .queryParam("email", result.email())
                .queryParam("name", result.name())
                .queryParam("profileImageUrl", result.profileImageUrl())
                .encode()
                .build()
                .toUriString();
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("Google OAuth 콜백 처리 실패", e);
            response.sendRedirect(googleProperties.getFrontendRedirectUri() + "?error=oauth_failed");
        }
    }

    @Operation(
        summary = "Google 로그인",
        description = "tempToken으로 기존 Google 계정 로그인합니다. 연결된 사용자가 비활성화된 경우 로그인할 수 없습니다."
    )
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        return ResponseEntity.ok(googleAuthService.login(request.tempToken()));
    }

    @Operation(
        summary = "Google 계정과 Local 계정 연결",
        description = "tempToken으로 Google 계정을 Local 계정과 연결합니다. 비활성화된 Local 계정에는 연결할 수 없습니다."
    )
    @PostMapping("/connect")
    public ResponseEntity<TokenResponse> connectLocalAccount(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        return ResponseEntity.ok(googleAuthService.connectToLocalAccount(request));
    }

    @Operation(
        summary = "Google 회원가입",
        description = """
            tempToken과 이름, 전화번호, 생년월일로 Google 계정을 등록합니다.
            생년월일은 내부 저장 형식으로 변환되며, 동일 이메일의 비활성 계정이 존재하면 해당 계정을 재사용하거나 새로 가입할 수 없습니다.
            동일 이메일의 Local 계정이 존재하면 기존 사용자에게 Google 계정이 자동으로 연결되며, 기존 사용자 정보와 생년월일은 유지됩니다.
            """
    )
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(
            @Valid @RequestBody GoogleSignupRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(googleAuthService.signup(
                request.tempToken(),
                request.name(),
                request.phoneNumber(),
                request.birthDate()
            ));
    }
}
