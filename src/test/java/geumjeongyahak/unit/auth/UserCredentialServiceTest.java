package geumjeongyahak.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.auth.service.UserCredentialService;
import geumjeongyahak.domain.users.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserCredentialServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final String NEW_EMAIL = "new-user@test.com";
    private static final Long USER_ID = 10L;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserCredentialService userCredentialService;

    @BeforeEach
    void setUp() {
        userCredentialService = new UserCredentialService(userCredentialRepository, passwordEncoder);
    }

    @Test
    void updateLocalPassword_clearsPendingPasswordResetToken() {
        User user = User.builder()
            .name("비밀번호 변경 사용자")
            .email(EMAIL)
            .build();
        UserCredential credential = UserCredential.local(user, EMAIL, "old-password-hash", true);
        credential.issuePasswordResetToken("reset-token-hash", LocalDateTime.now().plusMinutes(15));
        given(userCredentialRepository.findByUserIdAndProvider(user.getId(), ProviderType.LOCAL))
            .willReturn(Optional.of(credential));

        userCredentialService.updateLocalPassword(user, "new-password-hash");

        assertThat(credential.getPasswordHash()).isEqualTo("new-password-hash");
        assertThat(credential.getPasswordResetTokenHash()).isNull();
        assertThat(credential.getPasswordResetTokenExpiresAt()).isNull();
        verify(userCredentialRepository).save(credential);
    }

    @Test
    void updateLocalCredentialEmail_clearsPendingPasswordResetToken() {
        User user = User.builder()
            .name("이메일 변경 사용자")
            .email(EMAIL)
            .build();
        UserCredential credential = UserCredential.local(user, EMAIL, "password-hash", true);
        credential.issuePasswordResetToken("reset-token-hash", LocalDateTime.now().plusMinutes(15));
        given(userCredentialRepository.findByUserIdAndProvider(user.getId(), ProviderType.LOCAL))
            .willReturn(Optional.of(credential));
        given(userCredentialRepository.existsByCredentialEmailAndProvider(NEW_EMAIL, ProviderType.LOCAL))
            .willReturn(false);

        userCredentialService.updateLocalCredentialEmail(user, NEW_EMAIL);

        assertThat(credential.getCredentialEmail()).isEqualTo(NEW_EMAIL);
        assertThat(credential.getPasswordResetTokenHash()).isNull();
        assertThat(credential.getPasswordResetTokenExpiresAt()).isNull();
        verify(userCredentialRepository).save(credential);
    }

    @Test
    void clearPasswordResetTokensByUserId_clearsAllCredentialResetTokens() {
        User user = User.builder()
            .name("비활성화 사용자")
            .email(EMAIL)
            .build();
        UserCredential localCredential = UserCredential.local(user, EMAIL, "password-hash", true);
        localCredential.issuePasswordResetToken("reset-token-hash", LocalDateTime.now().plusMinutes(15));
        given(userCredentialRepository.findAllByUserId(USER_ID)).willReturn(java.util.List.of(localCredential));

        userCredentialService.clearPasswordResetTokensByUserId(USER_ID);

        assertThat(localCredential.getPasswordResetTokenHash()).isNull();
        assertThat(localCredential.getPasswordResetTokenExpiresAt()).isNull();
        verify(userCredentialRepository).saveAll(java.util.List.of(localCredential));
    }
}
