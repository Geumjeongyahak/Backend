package geumjeongyahak.domain.auth.service;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.mail.MailProperties;
import geumjeongyahak.common.mail.MailSenderService;
import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.exception.AuthErrorCode;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PasswordResetService {

    private final UserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSenderService mailSenderService;
    private final Clock clock;
    private final Supplier<String> resetCodeSupplier;
    private final long expirationMinutes;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    @Autowired
    public PasswordResetService(
        UserCredentialRepository credentialRepository,
        PasswordEncoder passwordEncoder,
        MailSenderService mailSenderService,
        Clock clock,
        MailProperties mailProperties
    ) {
        this(
            credentialRepository,
            passwordEncoder,
            mailSenderService,
            clock,
            new NumericCodeGenerator(),
            mailProperties.passwordResetExpirationMinutes()
        );
    }

    public PasswordResetService(
        UserCredentialRepository credentialRepository,
        PasswordEncoder passwordEncoder,
        MailSenderService mailSenderService,
        Clock clock,
        Supplier<String> resetCodeSupplier
    ) {
        this(
            credentialRepository,
            passwordEncoder,
            mailSenderService,
            clock,
            resetCodeSupplier,
            15
        );
    }

    private PasswordResetService(
        UserCredentialRepository credentialRepository,
        PasswordEncoder passwordEncoder,
        MailSenderService mailSenderService,
        Clock clock,
        Supplier<String> resetCodeSupplier,
        long expirationMinutes
    ) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSenderService = mailSenderService;
        this.clock = clock;
        this.resetCodeSupplier = resetCodeSupplier;
        this.expirationMinutes = expirationMinutes;
    }

    @Transactional
    public void requestReset(String email) {
        credentialRepository.findByCredentialEmailAndProvider(email, ProviderType.LOCAL)
            .filter(credential -> !credential.getUser().isDeleted())
            .ifPresentOrElse(
                this::issueResetCode,
                () -> log.info("비밀번호 재설정 요청 수락 - 대상 로컬 계정 없음: email={}", email)
            );
    }

    @Transactional
    public void resetPassword(String email, String resetCode, String newPassword) {
        UserCredential credential = credentialRepository.findByCredentialEmailAndProvider(email, ProviderType.LOCAL)
            .filter(candidate -> !candidate.getUser().isDeleted())
            .orElseThrow(() -> new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        if (credential.getPasswordResetTokenHash() == null
            || !passwordEncoder.matches(resetCode, credential.getPasswordResetTokenHash())) {
            credential.recordPasswordResetFailure(MAX_FAILED_ATTEMPTS);
            credentialRepository.save(credential);
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (credential.isPasswordResetTokenExpired(now)) {
            credential.clearPasswordResetToken();
            credentialRepository.save(credential);
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        credential.completePasswordReset(passwordEncoder.encode(newPassword));
        credentialRepository.save(credential);
        log.info("비밀번호 재설정 완료: credentialId={}, userId={}", credential.getId(), credential.getUser().getId());
    }

    private void issueResetCode(UserCredential credential) {
        validateCooldown(credential.getPasswordResetRequestedAt());
        String resetCode = resetCodeSupplier.get();
        LocalDateTime requestedAt = LocalDateTime.now(clock);
        LocalDateTime expiresAt = requestedAt.plusMinutes(expirationMinutes);
        credential.issuePasswordResetToken(passwordEncoder.encode(resetCode), expiresAt, requestedAt);
        credentialRepository.save(credential);
        mailSenderService.sendPasswordResetMail(
            credential.getCredentialEmail(),
            credential.getUser().getName(),
            resetCode
        );
        log.info("비밀번호 재설정 메일 처리 완료: credentialId={}, expiresAt={}", credential.getId(), expiresAt);
    }

    private void validateCooldown(LocalDateTime requestedAt) {
        if (requestedAt != null && requestedAt.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "인증번호는 60초 후 다시 요청할 수 있습니다.");
        }
    }
}
