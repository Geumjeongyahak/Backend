package geumjeongyahak.domain.auth.service;

import geumjeongyahak.common.exception.BusinessException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class EmailVerificationService {

    private final UserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSenderService mailSenderService;
    private final Clock clock;
    private final Supplier<String> verificationCodeSupplier;
    private final long expirationMinutes;

    @Autowired
    public EmailVerificationService(
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
            mailProperties.emailVerificationExpirationMinutes()
        );
    }

    public EmailVerificationService(
        UserCredentialRepository credentialRepository,
        PasswordEncoder passwordEncoder,
        MailSenderService mailSenderService,
        Clock clock,
        Supplier<String> verificationCodeSupplier
    ) {
        this(
            credentialRepository,
            passwordEncoder,
            mailSenderService,
            clock,
            verificationCodeSupplier,
            15
        );
    }

    private EmailVerificationService(
        UserCredentialRepository credentialRepository,
        PasswordEncoder passwordEncoder,
        MailSenderService mailSenderService,
        Clock clock,
        Supplier<String> verificationCodeSupplier,
        long expirationMinutes
    ) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSenderService = mailSenderService;
        this.clock = clock;
        this.verificationCodeSupplier = verificationCodeSupplier;
        this.expirationMinutes = expirationMinutes;
    }

    @Transactional
    public void issueVerification(UserCredential credential) {
        if (credential.isEmailVerified()) {
            credential.clearEmailVerificationToken();
            credentialRepository.save(credential);
            log.info("이메일 인증 발급 생략 - 이미 인증됨: credentialId={}", credential.getId());
            return;
        }
        issueVerificationCode(credential);
    }

    @Transactional
    public void confirm(String email, String verificationCode) {
        UserCredential credential = credentialRepository.findByCredentialEmailAndProvider(email, ProviderType.LOCAL)
            .filter(candidate -> !candidate.getUser().isDeleted())
            .filter(candidate -> candidate.getEmailVerificationTokenHash() != null)
            .filter(candidate -> passwordEncoder.matches(verificationCode, candidate.getEmailVerificationTokenHash()))
            .orElseThrow(() -> new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID));

        LocalDateTime now = LocalDateTime.now(clock);
        if (credential.isEmailVerificationTokenExpired(now)) {
            credential.clearEmailVerificationToken();
            credentialRepository.save(credential);
            throw new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED);
        }

        credential.verifyEmail();
        credentialRepository.save(credential);
        log.info("이메일 인증 완료: credentialId={}, userId={}", credential.getId(), credential.getUser().getId());
    }

    @Transactional
    public void resend(String email) {
        credentialRepository.findByCredentialEmailAndProvider(email, ProviderType.LOCAL)
            .filter(credential -> !credential.getUser().isDeleted())
            .filter(credential -> !credential.isEmailVerified())
            .ifPresentOrElse(
                this::issueVerificationCode,
                () -> log.info("이메일 인증 재발송 요청 수락 - 대상 미인증 로컬 계정 없음: email={}", email)
            );
    }

    private void issueVerificationCode(UserCredential credential) {
        String verificationCode = verificationCodeSupplier.get();
        LocalDateTime requestedAt = LocalDateTime.now(clock);
        LocalDateTime expiresAt = requestedAt.plusMinutes(expirationMinutes);
        credential.issueEmailVerificationToken(passwordEncoder.encode(verificationCode), expiresAt, requestedAt);
        credentialRepository.save(credential);
        mailSenderService.sendEmailVerificationMail(
            credential.getCredentialEmail(),
            credential.getUser().getName(),
            verificationCode
        );
        log.info("이메일 인증 메일 처리 완료: credentialId={}, expiresAt={}", credential.getId(), expiresAt);
    }

}
