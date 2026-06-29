package geumjeongyahak.domain.auth.service;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.mail.MailProperties;
import geumjeongyahak.common.mail.MailSenderService;
import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.auth.enums.ProviderType;
import geumjeongyahak.domain.auth.exception.AuthErrorCode;
import geumjeongyahak.domain.auth.repository.UserCredentialRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class EmailVerificationService {

    private final UserCredentialRepository credentialRepository;
    private final MailSenderService mailSenderService;
    private final Clock clock;
    private final Supplier<String> verificationCodeSupplier;
    private final long expirationMinutes;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    @Autowired
    public EmailVerificationService(
        UserCredentialRepository credentialRepository,
        MailSenderService mailSenderService,
        Clock clock,
        MailProperties mailProperties
    ) {
        this(
            credentialRepository,
            mailSenderService,
            clock,
            new UrlTokenGenerator(),
            mailProperties.emailVerificationExpirationMinutes()
        );
    }

    public EmailVerificationService(
        UserCredentialRepository credentialRepository,
        MailSenderService mailSenderService,
        Clock clock,
        Supplier<String> verificationCodeSupplier
    ) {
        this(
            credentialRepository,
            mailSenderService,
            clock,
            verificationCodeSupplier,
            15
        );
    }

    private EmailVerificationService(
        UserCredentialRepository credentialRepository,
        MailSenderService mailSenderService,
        Clock clock,
        Supplier<String> verificationCodeSupplier,
        long expirationMinutes
    ) {
        this.credentialRepository = credentialRepository;
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
            .orElseThrow(() -> new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID));

        confirmCredential(credential, verificationCode);
    }

    @Transactional
    public void confirmByToken(String token) {
        UserCredential credential = credentialRepository.findByEmailVerificationTokenHash(hashToken(token))
            .filter(candidate -> !candidate.getUser().isDeleted())
            .orElseThrow(() -> new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID));

        confirmCredential(credential, token);
    }

    private void confirmCredential(UserCredential credential, String token) {
        if (credential.getEmailVerificationTokenHash() == null
            || !hashToken(token).equals(credential.getEmailVerificationTokenHash())) {
            credential.recordEmailVerificationFailure(MAX_FAILED_ATTEMPTS);
            credentialRepository.save(credential);
            throw new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
        }

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
        validateCooldown(credential.getEmailVerificationRequestedAt());
        String verificationToken = verificationCodeSupplier.get();
        LocalDateTime requestedAt = LocalDateTime.now(clock);
        LocalDateTime expiresAt = requestedAt.plusMinutes(expirationMinutes);
        credential.issueEmailVerificationToken(hashToken(verificationToken), expiresAt, requestedAt);
        credentialRepository.save(credential);
        try {
            mailSenderService.sendEmailVerificationMail(
                credential.getCredentialEmail(),
                credential.getUser().getName(),
                verificationToken
            );
        } catch (Exception exception) {
            log.warn(
                "이메일 인증 메일 발송 실패 - credentialId={}, email={}",
                credential.getId(),
                credential.getCredentialEmail(),
                exception
            );
        }
        log.info("이메일 인증 메일 처리 완료: credentialId={}, expiresAt={}", credential.getId(), expiresAt);
    }

    private void validateCooldown(LocalDateTime requestedAt) {
        if (requestedAt != null && requestedAt.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "인증번호는 60초 후 다시 요청할 수 있습니다.");
        }
    }

    private static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
