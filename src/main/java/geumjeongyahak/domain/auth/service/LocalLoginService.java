package geumjeongyahak.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.security.jwt.JwtTokenProvider;
import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.exception.InvalidRefreshTokenException;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.auth.v1.dto.request.LocalLoginRequest;
import geumjeongyahak.domain.auth.v1.dto.request.LocalSignupRequest;
import geumjeongyahak.domain.auth.v1.dto.request.RefreshTokenRequest;
import geumjeongyahak.domain.auth.v1.dto.response.TokenResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.DuplicateEmailException;
import geumjeongyahak.domain.users.service.UserCrudService;
import geumjeongyahak.domain.users.service.UserProxyService;

import java.time.LocalDateTime;
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalLoginService {
    private final UserProxyService userProxyService;
    private final UserCredentialRepository userCredentialRepository;
    private final UserCredentialService userCredentialService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public TokenResponse login(LocalLoginRequest request) {
        log.debug("로그인 시도: {}", request.email());

        UserCredential credential = userCredentialRepository.findByCredentialEmailAndProvider(request.email(), ProviderType.LOCAL)
            .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));
        User user = credential.getUser();

        if (credential.getPasswordHash() == null || !passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            log.warn("로그인 실패 - 잘못된 비밀번호: {}", request.email());
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        credential.setLastLoginAt(LocalDateTime.now());
        TokenResponse tokenResponse = createTokenResponse(user, credential.getId());
        log.info("로그인 성공: userId={}, email={}", user.getId(), credential.getCredentialEmail());
        return tokenResponse;
    }

    @Transactional
    public TokenResponse signup(LocalSignupRequest request) {
        log.debug("회원가입 시도: {}", request.email());

        if (request.email() != null && userProxyService.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        User user = User.builder()
            .name(request.name())
            .nickname(UserCrudService.generateUniqueNickname(request.name(), userProxyService))
            .email(request.email())
            .phoneNumber(request.phoneNumber())
            .role(RoleType.VOLUNTEER)
            .build();

        User savedUser = userProxyService.save(user);
        UserCredential credential = userCredentialService.createLocalCredential(
            savedUser,
            request.email(),
            request.password()
        );
        TokenResponse tokenResponse = createTokenResponse(savedUser, credential.getId());
        log.info("회원가입 성공: userId={}, email={}", savedUser.getId(), request.email());
        return tokenResponse;
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        log.debug("토큰 재발급 시도");
        // Refresh Token 검증
        if (!refreshTokenService.validateRefreshToken(request.refreshToken())) {
            throw new InvalidRefreshTokenException();
        }
        // 사용자 조회
        Long credentialId = refreshTokenService.getCredentialIdFromRefreshToken(request.refreshToken())
                .orElseThrow(InvalidRefreshTokenException::new);
        UserCredential credential = userCredentialRepository.findById(credentialId)
            .orElseThrow(InvalidRefreshTokenException::new);
        User user = credential.getUser();

        // 새로운 토큰 생성 (Refresh Token Rotation)
        TokenResponse tokenResponse = createTokenResponse(user, credentialId);

        log.info("토큰 재발급 성공: credentialId={}, userId={}", credentialId, user.getId());
        return tokenResponse;
    }

    @Transactional
    public void logout(String refreshToken) {
        log.info("로그아웃 시도");

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.deleteRefreshToken(refreshToken);
            log.info("로그아웃 성공: refreshToken 삭제됨");
        }
    }

    @Transactional
    public void logoutAllDevices(Long userId) {
        log.info("전체 디바이스 로그아웃: userId={}", userId);
        refreshTokenService.deleteRefreshTokenByUserId(userId);
    }

    private TokenResponse createTokenResponse(User user, Long credentialId) {
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(user.getId()));
        String refreshToken = refreshTokenService.createRefreshToken(credentialId);
        LocalDateTime accessExpiresAt = jwtTokenProvider.getAccessTokenExpiresAt();
        LocalDateTime refreshExpiresAt = refreshTokenService.getRefreshTokenExpiresAt();

        return TokenResponse.of(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);
    }
}
