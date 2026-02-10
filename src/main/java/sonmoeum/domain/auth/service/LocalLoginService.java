package sonmoeum.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.common.security.jwt.JwtTokenProvider;
import sonmoeum.domain.auth.enums.RoleType;
import sonmoeum.domain.auth.exception.InvalidRefreshTokenException;
import sonmoeum.domain.auth.v1.dto.request.LocalLoginRequest;
import sonmoeum.domain.auth.v1.dto.request.LocalSignupRequest;
import sonmoeum.domain.auth.v1.dto.request.RefreshTokenRequest;
import sonmoeum.domain.auth.v1.dto.response.TokenResponse;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.exception.DuplicateEmailException;
import sonmoeum.domain.users.exception.DuplicateUsernameException;
import sonmoeum.domain.users.service.UserProxyService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalLoginService {
    private final UserProxyService userProxyService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public TokenResponse login(LocalLoginRequest request) {
        log.debug("로그인 시도: {}", request.username());

        // 사용자 조회
        User user = userProxyService.getByUsername(request.username());

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("로그인 실패 - 잘못된 비밀번호: {}", request.username());
            throw new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        // 토큰 생성
        TokenResponse tokenResponse = createTokenResponse(user);
        log.info("로그인 성공: userId={}, username={}", user.getId(), user.getUsername());
        return tokenResponse;
    }

    @Transactional
    public TokenResponse signup(LocalSignupRequest request) {
        log.debug("회원가입 시도: {}", request.username());

        // 중복 확인
        if (userProxyService.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }

        if (request.email() != null && userProxyService.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        // 사용자 생성
        User user = User.localBuilder()
            .name(request.name())
            .username(request.username())
            .passwordHash(passwordEncoder.encode(request.password()))
            .email(request.email())
            .phoneNumber(request.phoneNumber())
            .roles(List.of(RoleType.ROLE_VOLUNTEER)) // 기본 역할: VOLUNTEER
            .build();

        userProxyService.save(user);
        // 토큰 생성
        TokenResponse tokenResponse = createTokenResponse(user);
        log.info("회원가입 성공: userId={}, username={}", user.getId(), user.getUsername());
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
        Long userId = refreshTokenService.getUserIdFromRefreshToken(request.refreshToken())
                .orElseThrow(InvalidRefreshTokenException::new);

        User user = userProxyService.getById(userId);

        // 새로운 토큰 생성 (Refresh Token Rotation)
        TokenResponse tokenResponse = createTokenResponse(user);

        log.info("토큰 재발급 성공: userId={}", userId);
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

    private TokenResponse createTokenResponse(User user) {
        // Access Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getUsername());
        // Refresh Token 생성
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // 만료 시각 계산
        LocalDateTime accessExpiresAt = jwtTokenProvider.getAccessTokenExpiresAt();
        LocalDateTime refreshExpiresAt = refreshTokenService.getRefreshTokenExpiresAt();

        return TokenResponse.of(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);
    }
}
