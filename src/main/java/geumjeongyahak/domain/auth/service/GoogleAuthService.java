package geumjeongyahak.domain.auth.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import geumjeongyahak.common.config.GoogleOAuth2Properties;
import geumjeongyahak.common.security.jwt.JwtTokenProvider;
import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.exception.OAuthProcessingException;
import geumjeongyahak.domain.auth.external.GoogleApiClient;
import geumjeongyahak.domain.auth.external.dto.GoogleTokenResponse;
import geumjeongyahak.domain.auth.external.dto.GoogleUserInfo;
import geumjeongyahak.domain.auth.v1.dto.request.GoogleLoginRequest;
import geumjeongyahak.domain.auth.v1.dto.response.GoogleCallbackResponse;
import geumjeongyahak.domain.auth.v1.dto.response.TokenResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";

    private final GoogleOAuth2Properties googleProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserProxyService userProxyService;
    private final UserCredentialService userCredentialService;
    private final RefreshTokenService refreshTokenService;
    private final GoogleApiClient googleApiClient;

    public String getAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString(GOOGLE_AUTH_URL)
            .queryParam("client_id", googleProperties.getClientId())
            .queryParam("redirect_uri", googleProperties.getRedirectUri())
            .queryParam("response_type", "code")
            .queryParam("scope", "openid email profile")
            .toUriString();
    }

    public GoogleCallbackResponse handleCallback(String code) {
        GoogleTokenResponse tokenResponse = googleApiClient.exchangeCode(code);
        GoogleUserInfo userInfo = googleApiClient.verifyIdToken(tokenResponse.idToken());
        boolean signupRequired = !userCredentialService.existsByCredentialEmailAndProvider(userInfo.email(), ProviderType.GOOGLE);
        boolean connectedToLocal = userCredentialService.existsByCredentialEmailAndProvider(userInfo.email(), ProviderType.LOCAL);

        log.info("Google 콜백 처리: email={}, signupRequired={}", userInfo.email(), signupRequired);
        return new GoogleCallbackResponse(
            jwtTokenProvider.createOAuth2TempToken(userInfo.sub(), userInfo.email()),
            signupRequired,
            connectedToLocal,
            userInfo.email(),
            userInfo.name(),
            userInfo.profileImageUrl()
        );
    }

    @Transactional
    public TokenResponse login(String tempToken) {
        String[] parts = extractTempToken(tempToken);
        String googleSub = parts[0];

        UserCredential credential = userCredentialService
            .getCredentialByProviderUserIdAndProvider(googleSub, ProviderType.GOOGLE);

        credential.setLastLoginAt(LocalDateTime.now());

        log.info("Google 로그인 성공: userId={}", credential.getUser().getId());
        return issueToken(credential);
    }

    @Transactional
    public TokenResponse connectToLocalAccount(GoogleLoginRequest request) {
        String[] parts = extractTempToken(request.tempToken());
        String googleSub = parts[0];
        String email = parts[1];

        User user = userCredentialService
            .findOptionalByCredentialEmailAndProvider(email, ProviderType.LOCAL)
            .map(UserCredential::getUser)
            .orElseThrow(() -> new OAuthProcessingException("연결할 로컬 계정을 찾을 수 없습니다."));

        UserCredential credential = userCredentialService.createGoogleCredential(user, googleSub, email, false);
        credential.setLastLoginAt(LocalDateTime.now());

        log.info("Google 계정 연결 성공: userId={}, email={}", user.getId(), email);
        return issueToken(credential);
    }

    @Transactional
    public TokenResponse signup(
        String tempToken,
        String name,
        String phoneNumber,
        String residentRegistrationNumberPrefix
    ) {
        String[] parts = extractTempToken(tempToken);
        String googleSub = parts[0];
        String email = parts[1];

        User user = userCredentialService
            .findOptionalByCredentialEmailAndProvider(email, ProviderType.LOCAL)
            .map(UserCredential::getUser)
            .orElseGet(() -> userProxyService.save(User.builder()
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .residentRegistrationNumberPrefix(residentRegistrationNumberPrefix)
                .role(RoleType.GUEST)
                .build()));

        UserCredential credential = userCredentialService.createGoogleCredential(user, googleSub, email, true);
        credential.setLastLoginAt(LocalDateTime.now());

        log.info("Google 회원가입 완료: userId={}, email={}", user.getId(), email);
        return issueToken(credential);
    }

    private TokenResponse issueToken(UserCredential credential) {
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(credential.getUser().getId()));
        String refreshToken = refreshTokenService.createRefreshToken(credential.getId());
        return TokenResponse.of(
            accessToken, refreshToken,
            jwtTokenProvider.getAccessTokenExpiresAt(),
            refreshTokenService.getRefreshTokenExpiresAt()
        );
    }

    private String[] extractTempToken(String tempToken) {
        if (!jwtTokenProvider.validate(tempToken)) {
            throw new OAuthProcessingException("유효하지 않은 임시 토큰입니다.");
        }
        String googleSub = jwtTokenProvider.getSubject(tempToken);
        String email = jwtTokenProvider.getEmail(tempToken);

        if (googleSub == null || email == null) {
            throw new OAuthProcessingException("임시 토큰에서 필요한 정보를 추출할 수 없습니다.");
        }
        return new String[]{googleSub, email};
    }
}
