package geumjeongyahak.unit.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import geumjeongyahak.domain.auth.exception.CredentialNotFoundException;
import geumjeongyahak.domain.auth.exception.InvalidRefreshTokenException;
import geumjeongyahak.domain.auth.service.LocalAuthService;
import geumjeongyahak.domain.auth.service.RefreshTokenService;
import geumjeongyahak.domain.auth.service.UserCredentialService;
import geumjeongyahak.domain.auth.v1.dto.request.RefreshTokenRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthService 토큰 재발급 단위 테스트")
class LocalAuthServiceRefreshTest {

    private static final String REFRESH_TOKEN = "refresh-token";
    private static final Long CREDENTIAL_ID = 10L;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserCredentialService userCredentialService;

    @InjectMocks
    private LocalAuthService localAuthService;

    @BeforeEach
    void validToken() {
        given(refreshTokenService.validateRefreshToken(REFRESH_TOKEN)).willReturn(true);
        given(refreshTokenService.getCredentialIdFromRefreshToken(REFRESH_TOKEN)).willReturn(Optional.of(CREDENTIAL_ID));
    }

    @Test
    @DisplayName("자격 증명이 없으면 유효하지 않은 토큰으로 답한다")
    void refresh_credentialMissing_throwsInvalidRefreshToken() {
        given(userCredentialService.getById(CREDENTIAL_ID)).willThrow(new CredentialNotFoundException());

        assertThatThrownBy(() -> localAuthService.refreshToken(new RefreshTokenRequest(REFRESH_TOKEN)))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("DB 오류는 토큰 오류로 바꾸지 않고 그대로 올린다")
    void refresh_databaseFailure_propagates() {
        given(userCredentialService.getById(CREDENTIAL_ID))
            .willThrow(new DataAccessResourceFailureException("connection refused"));

        assertThatThrownBy(() -> localAuthService.refreshToken(new RefreshTokenRequest(REFRESH_TOKEN)))
            .isInstanceOf(DataAccessResourceFailureException.class);
    }
}
