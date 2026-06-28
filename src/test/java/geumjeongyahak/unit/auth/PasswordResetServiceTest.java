package geumjeongyahak.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.mail.MailDeliveryResult;
import geumjeongyahak.common.mail.MailSenderService;
import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import geumjeongyahak.domain.auth.service.PasswordResetService;
import geumjeongyahak.domain.users.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-25T10:15:30Z"), ZoneId.of("Asia/Seoul"));
    private static final String EMAIL = "reset@test.com";
    private static final String RAW_TOKEN = "123456";
    private static final String TOKEN_HASH = "hashed-token";

    @Mock
    private UserCredentialRepository credentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailSenderService mailSenderService;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
            credentialRepository,
            passwordEncoder,
            mailSenderService,
            CLOCK,
            () -> RAW_TOKEN
        );
    }

    @Test
    void requestReset_storesHashedTokenAndSendsPasswordResetMailWhenLocalUserExists() {
        User user = User.builder()
            .name("비밀번호 재설정 사용자")
            .email(EMAIL)
            .build();
        UserCredential credential = UserCredential.local(user, EMAIL, "old-password-hash", true);
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));
        given(passwordEncoder.encode(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(mailSenderService.sendPasswordResetMail(eq(EMAIL), eq("비밀번호 재설정 사용자"), eq(RAW_TOKEN)))
            .willReturn(MailDeliveryResult.sent("password-reset", EMAIL));

        passwordResetService.requestReset(EMAIL);

        assertThat(credential.getPasswordResetTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(credential.getPasswordResetTokenExpiresAt()).isEqualTo(CLOCK.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime().plusMinutes(15));
        verify(credentialRepository).save(credential);
        verify(mailSenderService).sendPasswordResetMail(EMAIL, "비밀번호 재설정 사용자", RAW_TOKEN);
    }

    @Test
    void requestReset_rejectsRapidRepeatRequest() {
        User user = User.builder()
            .name("비밀번호 재설정 사용자")
            .email(EMAIL)
            .build();
        UserCredential credential = UserCredential.local(user, EMAIL, "old-password-hash", true);
        credential.issuePasswordResetToken(
            TOKEN_HASH,
            CLOCK.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime().plusMinutes(15),
            CLOCK.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime().minusSeconds(30)
        );
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));

        assertThatThrownBy(() -> passwordResetService.requestReset(EMAIL))
            .isInstanceOf(BusinessException.class)
            .hasMessage("인증번호는 60초 후 다시 요청할 수 있습니다.");

        verify(mailSenderService, never()).sendPasswordResetMail(any(), any(), any());
    }

    @Test
    void requestReset_returnsAcceptedWithoutSendingMailWhenAccountDoesNotExist() {
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.empty());

        passwordResetService.requestReset(EMAIL);

        verify(mailSenderService, never()).sendPasswordResetMail(any(), any(), any());
        verify(credentialRepository, never()).save(any());
    }

    @Test
    void resetPassword_updatesPasswordAndClearsResetTokenWhenTokenMatches() {
        User user = User.builder()
            .name("비밀번호 변경 사용자")
            .email(EMAIL)
            .build();
        UserCredential credential = UserCredential.local(user, EMAIL, "old-password-hash", true);
        credential.issuePasswordResetToken(TOKEN_HASH, CLOCK.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime().plusMinutes(15));
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));
        given(passwordEncoder.matches(RAW_TOKEN, TOKEN_HASH)).willReturn(true);
        given(passwordEncoder.encode("new-password123!")).willReturn("new-password-hash");

        passwordResetService.resetPassword(EMAIL, RAW_TOKEN, "new-password123!");

        assertThat(credential.getPasswordHash()).isEqualTo("new-password-hash");
        assertThat(credential.getPasswordResetTokenHash()).isNull();
        assertThat(credential.getPasswordResetTokenExpiresAt()).isNull();
        verify(credentialRepository).save(credential);
    }

    @Test
    void resetPassword_rejectsExpiredToken() {
        User user = User.builder()
            .name("만료 사용자")
            .email(EMAIL)
            .build();
        UserCredential credential = UserCredential.local(user, EMAIL, "old-password-hash", true);
        credential.issuePasswordResetToken(TOKEN_HASH, CLOCK.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime().minusMinutes(1));
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));
        given(passwordEncoder.matches(RAW_TOKEN, TOKEN_HASH)).willReturn(true);

        assertThatThrownBy(() -> passwordResetService.resetPassword(EMAIL, RAW_TOKEN, "new-password123!"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("비밀번호 재설정 인증번호가 만료되었습니다.");
    }

    @Test
    void resetPassword_rejectsInvalidResetCode() {
        User user = User.builder()
            .name("인증번호 불일치 사용자")
            .email(EMAIL)
            .build();
        UserCredential credential = UserCredential.local(user, EMAIL, "old-password-hash", true);
        credential.issuePasswordResetToken(TOKEN_HASH, CLOCK.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime().plusMinutes(15));
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));
        given(passwordEncoder.matches("654321", TOKEN_HASH)).willReturn(false);

        assertThatThrownBy(() -> passwordResetService.resetPassword(EMAIL, "654321", "new-password123!"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("유효하지 않은 비밀번호 재설정 인증번호입니다.");

        assertThat(credential.getPasswordHash()).isEqualTo("old-password-hash");
        assertThat(credential.getPasswordResetTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(credential.getPasswordResetFailedAttempts()).isEqualTo(1);
        verify(credentialRepository).save(credential);
    }

    @Test
    void resetPassword_clearsTokenAfterFiveInvalidCodes() {
        User user = User.builder()
            .name("인증번호 불일치 사용자")
            .email(EMAIL)
            .build();
        UserCredential credential = UserCredential.local(user, EMAIL, "old-password-hash", true);
        credential.issuePasswordResetToken(TOKEN_HASH, CLOCK.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime().plusMinutes(15));
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));
        given(passwordEncoder.matches("654321", TOKEN_HASH)).willReturn(false);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> passwordResetService.resetPassword(EMAIL, "654321", "new-password123!"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("유효하지 않은 비밀번호 재설정 인증번호입니다.");
        }

        assertThat(credential.getPasswordResetTokenHash()).isNull();
        assertThat(credential.getPasswordResetTokenExpiresAt()).isNull();
        assertThat(credential.getPasswordResetFailedAttempts()).isZero();
    }

    @Test
    void resetPassword_rejectsDeactivatedUserEvenWhenTokenMatches() {
        User user = User.builder()
            .name("비활성 사용자")
            .email(EMAIL)
            .build();
        user.softDelete();
        UserCredential credential = UserCredential.local(user, EMAIL, "old-password-hash", true);
        credential.issuePasswordResetToken(TOKEN_HASH, CLOCK.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime().plusMinutes(15));
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));

        assertThatThrownBy(() -> passwordResetService.resetPassword(EMAIL, RAW_TOKEN, "new-password123!"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("유효하지 않은 비밀번호 재설정 인증번호입니다.");

        assertThat(credential.getPasswordHash()).isEqualTo("old-password-hash");
        verify(credentialRepository, never()).save(any());
    }
}
