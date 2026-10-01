package geumjeongyahak.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import geumjeongyahak.common.security.config.SecurityProperties;
import geumjeongyahak.domain.auth.entity.RefreshToken;
import geumjeongyahak.domain.auth.repository.RefreshTokenRepository;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.auth.service.RefreshTokenService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService 기기 수 상한 단위 테스트")
class RefreshTokenServiceTest {

    private static final Long CREDENTIAL_ID = 7L;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setRefreshExpSeconds(3600);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, userCredentialRepository, properties);
    }

    private RefreshToken token(String value, int createdMinutesAgo, boolean expired) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = RefreshToken.builder()
            .token(value)
            .credentialId(CREDENTIAL_ID)
            .expiryDate(expired ? now.minusMinutes(1) : now.plusDays(1))
            .build();
        ReflectionTestUtils.setField(token, "createdAt", now.minusMinutes(createdMinutesAgo));
        return token;
    }

    @SuppressWarnings("unchecked")
    private List<String> deletedTokens() {
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(refreshTokenRepository).deleteAllByIdInBatch(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("살아 있는 토큰이 5개면 가장 오래된 것과 만료된 것을 지우고 저장한다")
    void atCap_deletesOldestAndExpired() {
        List<RefreshToken> existing = new ArrayList<>();
        existing.add(token("expired", 1, true));
        for (int i = 1; i <= 5; i++) {
            existing.add(token("live-" + i, i * 10, false)); // live-5 가장 오래됨
        }
        given(refreshTokenRepository.findByCredentialId(CREDENTIAL_ID)).willReturn(existing);

        String created = refreshTokenService.createRefreshToken(CREDENTIAL_ID);

        assertThat(deletedTokens()).containsExactlyInAnyOrder("expired", "live-5");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertThat(created).isNotBlank();
    }

    @Test
    @DisplayName("상한 아래면 만료된 토큰만 지운다")
    void belowCap_deletesOnlyExpired() {
        given(refreshTokenRepository.findByCredentialId(CREDENTIAL_ID))
            .willReturn(List.of(token("expired", 30, true), token("live-1", 10, false), token("live-2", 20, false)));

        refreshTokenService.createRefreshToken(CREDENTIAL_ID);

        assertThat(deletedTokens()).containsExactly("expired");
    }

    @Test
    @DisplayName("정리할 토큰이 없으면 삭제 쿼리를 보내지 않는다")
    void nothingToDelete_skipsDelete() {
        given(refreshTokenRepository.findByCredentialId(CREDENTIAL_ID))
            .willReturn(List.of(token("live-1", 10, false)));

        refreshTokenService.createRefreshToken(CREDENTIAL_ID);

        verify(refreshTokenRepository, never()).deleteAllByIdInBatch(anyList());
    }
}
