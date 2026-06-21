package geumjeongyahak.unit.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import geumjeongyahak.common.config.GoogleOAuth2Properties;
import geumjeongyahak.common.security.jwt.JwtTokenProvider;
import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.auth.exception.OAuthProcessingException;
import geumjeongyahak.domain.auth.external.GoogleApiClient;
import geumjeongyahak.domain.auth.service.GoogleAuthService;
import geumjeongyahak.domain.auth.service.RefreshTokenService;
import geumjeongyahak.domain.auth.service.UserCredentialService;
import geumjeongyahak.domain.auth.v1.dto.request.GoogleLoginRequest;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final String TEMP_TOKEN = "google-temp-token";
    private static final String GOOGLE_SUB = "google-user-id";
    private static final String EMAIL = "deactivated-google@test.com";

    @Mock
    private GoogleOAuth2Properties googleProperties;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserProxyService userProxyService;

    @Mock
    private UserCredentialService userCredentialService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private GoogleApiClient googleApiClient;

    @InjectMocks
    private GoogleAuthService googleAuthService;

    private User deactivatedUser;

    @BeforeEach
    void setUp() {
        deactivatedUser = User.builder()
            .name("비활성 Google 사용자")
            .email(EMAIL)
            .role(RoleType.GUEST)
            .build();
        deactivatedUser.softDelete();

        given(jwtTokenProvider.validate(TEMP_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getSubject(TEMP_TOKEN)).willReturn(GOOGLE_SUB);
        given(jwtTokenProvider.getEmail(TEMP_TOKEN)).willReturn(EMAIL);
    }

    @Test
    void login_rejectsDeactivatedUserBeforeIssuingToken() {
        UserCredential credential = UserCredential.google(
            deactivatedUser,
            GOOGLE_SUB,
            EMAIL,
            true
        );
        given(userCredentialService.getCredentialByProviderUserIdAndProvider(
            GOOGLE_SUB,
            ProviderType.GOOGLE
        )).willReturn(credential);

        assertThatThrownBy(() -> googleAuthService.login(TEMP_TOKEN))
            .isInstanceOf(OAuthProcessingException.class)
            .hasMessage("비활성화된 사용자입니다.");

        verify(refreshTokenService, never()).createRefreshToken(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void connectToLocalAccount_rejectsDeactivatedUserBeforeCreatingGoogleCredential() {
        given(userCredentialService.findOptionalByCredentialEmailAndProvider(
            EMAIL,
            ProviderType.LOCAL
        )).willReturn(Optional.of(UserCredential.local(
            deactivatedUser,
            EMAIL,
            "encoded-password",
            true
        )));

        assertThatThrownBy(() -> googleAuthService.connectToLocalAccount(
            new GoogleLoginRequest(TEMP_TOKEN)
        ))
            .isInstanceOf(OAuthProcessingException.class)
            .hasMessage("비활성화된 사용자입니다.");

        verify(userCredentialService, never()).createGoogleCredential(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyBoolean()
        );
    }

    @Test
    void signup_rejectsDeactivatedLocalUserBeforeCreatingGoogleCredential() {
        given(userCredentialService.findOptionalByCredentialEmailAndProvider(
            EMAIL,
            ProviderType.LOCAL
        )).willReturn(Optional.of(UserCredential.local(
            deactivatedUser,
            EMAIL,
            "encoded-password",
            true
        )));

        assertThatThrownBy(() -> googleAuthService.signup(
            TEMP_TOKEN,
            "비활성 Google 사용자",
            "010-1234-5678",
            "000101"
        ))
            .isInstanceOf(OAuthProcessingException.class)
            .hasMessage("비활성화된 사용자입니다.");

        verify(userProxyService, never()).save(org.mockito.ArgumentMatchers.any());
        verify(userCredentialService, never()).createGoogleCredential(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyBoolean()
        );
    }
}
