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
import geumjeongyahak.domain.auth.service.EmailVerificationService;
import geumjeongyahak.domain.users.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-25T10:15:30Z"), ZoneId.of("Asia/Seoul"));
    private static final String EMAIL = "verify@test.com";
    private static final String RAW_CODE = "123456";
    private static final String TOKEN_HASH = "hashed-verification-token";

    @Mock
    private UserCredentialRepository credentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailSenderService mailSenderService;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService(
            credentialRepository,
            passwordEncoder,
            mailSenderService,
            CLOCK,
            () -> RAW_CODE
        );
    }

    @Test
    void issueVerification_storesHashedTokenAndSendsEmailVerificationMail() {
        UserCredential credential = unverifiedCredential();
        given(passwordEncoder.encode(RAW_CODE)).willReturn(TOKEN_HASH);
        given(mailSenderService.sendEmailVerificationMail(eq(EMAIL), eq("이메일 인증 사용자"), eq(RAW_CODE)))
            .willReturn(MailDeliveryResult.sent("email-verification", EMAIL));

        emailVerificationService.issueVerification(credential);

        assertThat(credential.getEmailVerificationTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(credential.getEmailVerificationTokenExpiresAt())
            .isEqualTo(LocalDateTime.now(CLOCK).plusMinutes(15));
        assertThat(credential.getEmailVerificationRequestedAt())
            .isEqualTo(LocalDateTime.now(CLOCK));
        verify(credentialRepository).save(credential);
        verify(mailSenderService).sendEmailVerificationMail(EMAIL, "이메일 인증 사용자", RAW_CODE);
    }

    @Test
    void issueVerification_keepsTokenWhenMailDeliveryFails() {
        UserCredential credential = unverifiedCredential();
        given(passwordEncoder.encode(RAW_CODE)).willReturn(TOKEN_HASH);
        given(mailSenderService.sendEmailVerificationMail(eq(EMAIL), eq("이메일 인증 사용자"), eq(RAW_CODE)))
            .willThrow(new RuntimeException("smtp down"));

        emailVerificationService.issueVerification(credential);

        assertThat(credential.getEmailVerificationTokenHash()).isEqualTo(TOKEN_HASH);
        verify(credentialRepository).save(credential);
    }

    @Test
    void confirm_marksEmailVerifiedAndClearsVerificationToken() {
        UserCredential credential = unverifiedCredential();
        credential.issueEmailVerificationToken(TOKEN_HASH, LocalDateTime.now(CLOCK).plusMinutes(15), LocalDateTime.now(CLOCK));
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));
        given(passwordEncoder.matches(RAW_CODE, TOKEN_HASH)).willReturn(true);

        emailVerificationService.confirm(EMAIL, RAW_CODE);

        assertThat(credential.isEmailVerified()).isTrue();
        assertThat(credential.getEmailVerificationTokenHash()).isNull();
        assertThat(credential.getEmailVerificationTokenExpiresAt()).isNull();
        assertThat(credential.getEmailVerificationRequestedAt()).isNull();
        verify(credentialRepository).save(credential);
    }

    @Test
    void confirm_rejectsExpiredTokenAndClearsVerificationToken() {
        UserCredential credential = unverifiedCredential();
        credential.issueEmailVerificationToken(TOKEN_HASH, LocalDateTime.now(CLOCK).minusMinutes(1), LocalDateTime.now(CLOCK).minusMinutes(16));
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));
        given(passwordEncoder.matches(RAW_CODE, TOKEN_HASH)).willReturn(true);

        assertThatThrownBy(() -> emailVerificationService.confirm(EMAIL, RAW_CODE))
            .isInstanceOf(BusinessException.class)
            .hasMessage("이메일 인증번호가 만료되었습니다.");

        assertThat(credential.isEmailVerified()).isFalse();
        assertThat(credential.getEmailVerificationTokenHash()).isNull();
        verify(credentialRepository).save(credential);
    }

    @Test
    void confirm_rejectsInvalidCodeWithoutChangingState() {
        UserCredential credential = unverifiedCredential();
        credential.issueEmailVerificationToken(TOKEN_HASH, LocalDateTime.now(CLOCK).plusMinutes(15), LocalDateTime.now(CLOCK));
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));
        given(passwordEncoder.matches("654321", TOKEN_HASH)).willReturn(false);

        assertThatThrownBy(() -> emailVerificationService.confirm(EMAIL, "654321"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("유효하지 않은 이메일 인증번호입니다.");

        assertThat(credential.isEmailVerified()).isFalse();
        assertThat(credential.getEmailVerificationTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(credential.getEmailVerificationFailedAttempts()).isEqualTo(1);
        verify(credentialRepository).save(credential);
    }

    @Test
    void confirm_clearsTokenAfterFiveInvalidCodes() {
        UserCredential credential = unverifiedCredential();
        credential.issueEmailVerificationToken(TOKEN_HASH, LocalDateTime.now(CLOCK).plusMinutes(15), LocalDateTime.now(CLOCK));
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));
        given(passwordEncoder.matches("654321", TOKEN_HASH)).willReturn(false);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> emailVerificationService.confirm(EMAIL, "654321"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("유효하지 않은 이메일 인증번호입니다.");
        }

        assertThat(credential.isEmailVerified()).isFalse();
        assertThat(credential.getEmailVerificationTokenHash()).isNull();
        assertThat(credential.getEmailVerificationFailedAttempts()).isZero();
    }

    @Test
    void resend_replacesExistingVerificationCode() {
        emailVerificationService = new EmailVerificationService(
            credentialRepository,
            passwordEncoder,
            mailSenderService,
            CLOCK,
            () -> "222222"
        );
        UserCredential credential = unverifiedCredential();
        credential.issueEmailVerificationToken(
            "old-hash",
            LocalDateTime.now(CLOCK).plusMinutes(15),
            LocalDateTime.now(CLOCK).minusSeconds(61)
        );
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));
        given(passwordEncoder.encode("222222")).willReturn("new-hash");
        given(mailSenderService.sendEmailVerificationMail(eq(EMAIL), eq("이메일 인증 사용자"), eq("222222")))
            .willReturn(MailDeliveryResult.sent("email-verification", EMAIL));

        emailVerificationService.resend(EMAIL);

        assertThat(credential.getEmailVerificationTokenHash()).isEqualTo("new-hash");
        verify(mailSenderService).sendEmailVerificationMail(EMAIL, "이메일 인증 사용자", "222222");
    }

    @Test
    void resend_rejectsRapidRepeatRequest() {
        UserCredential credential = unverifiedCredential();
        credential.issueEmailVerificationToken(
            "old-hash",
            LocalDateTime.now(CLOCK).plusMinutes(15),
            LocalDateTime.now(CLOCK).minusSeconds(30)
        );
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));

        assertThatThrownBy(() -> emailVerificationService.resend(EMAIL))
            .isInstanceOf(BusinessException.class)
            .hasMessage("인증번호는 60초 후 다시 요청할 수 있습니다.");

        verify(mailSenderService, never()).sendEmailVerificationMail(any(), any(), any());
    }

    @Test
    void resend_returnsAcceptedWithoutSendingMailWhenAccountDoesNotExist() {
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.empty());

        emailVerificationService.resend(EMAIL);

        verify(mailSenderService, never()).sendEmailVerificationMail(any(), any(), any());
        verify(credentialRepository, never()).save(any());
    }

    @Test
    void emailVerificationDoesNotClearPasswordResetToken() {
        UserCredential credential = unverifiedCredential();
        credential.issuePasswordResetToken("reset-token-hash", LocalDateTime.now(CLOCK).plusMinutes(15));
        credential.issueEmailVerificationToken(TOKEN_HASH, LocalDateTime.now(CLOCK).plusMinutes(15), LocalDateTime.now(CLOCK));
        given(credentialRepository.findByCredentialEmailAndProvider(EMAIL, ProviderType.LOCAL))
            .willReturn(Optional.of(credential));
        given(passwordEncoder.matches(RAW_CODE, TOKEN_HASH)).willReturn(true);

        emailVerificationService.confirm(EMAIL, RAW_CODE);

        assertThat(credential.getPasswordResetTokenHash()).isEqualTo("reset-token-hash");
        assertThat(credential.getPasswordResetTokenExpiresAt()).isEqualTo(LocalDateTime.now(CLOCK).plusMinutes(15));
    }

    private UserCredential unverifiedCredential() {
        User user = User.builder()
            .name("이메일 인증 사용자")
            .email(EMAIL)
            .build();
        return UserCredential.local(user, EMAIL, "password-hash", false);
    }
}
