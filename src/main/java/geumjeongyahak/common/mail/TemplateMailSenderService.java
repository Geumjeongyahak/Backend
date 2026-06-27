package geumjeongyahak.common.mail;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateMailSenderService implements MailSenderService {

    private static final String SIGNUP_WELCOME_TEMPLATE = "mail/signup-welcome";
    private static final String PASSWORD_RESET_TEMPLATE = "mail/password-reset";
    private static final String EMAIL_VERIFICATION_TEMPLATE = "mail/email-verification";

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;

    @Override
    public MailDeliveryResult sendSignupWelcomeMail(String recipientEmail, String recipientName) {
        return sendHtmlMail(
            "signup-welcome",
            SIGNUP_WELCOME_TEMPLATE,
            recipientEmail,
            "금정야학 가입을 환영합니다",
            Map.of("name", defaultName(recipientName))
        );
    }

    @Override
    public MailDeliveryResult sendPasswordResetMail(String recipientEmail, String recipientName, String resetCode) {
        String resetUrl = buildPasswordResetUrl(resetCode);
        return sendHtmlMail(
            "password-reset",
            PASSWORD_RESET_TEMPLATE,
            recipientEmail,
            "금정야학 비밀번호 재설정 인증번호",
            Map.of(
                "name", defaultName(recipientName),
                "resetCode", resetCode,
                "resetUrl", resetUrl,
                "expiresMinutes", mailProperties.passwordResetExpirationMinutes()
            )
        );
    }

    @Override
    public MailDeliveryResult sendEmailVerificationMail(
        String recipientEmail,
        String recipientName,
        String verificationCode
    ) {
        String verificationUrl = buildEmailVerificationUrl(recipientEmail, verificationCode);
        return sendHtmlMail(
            "email-verification",
            EMAIL_VERIFICATION_TEMPLATE,
            recipientEmail,
            "금정야학 이메일 인증번호",
            Map.of(
                "name", defaultName(recipientName),
                "verificationCode", verificationCode,
                "verificationUrl", verificationUrl,
                "expiresMinutes", mailProperties.emailVerificationExpirationMinutes()
            )
        );
    }

    private MailDeliveryResult sendHtmlMail(
        String templateKey,
        String templatePath,
        String recipientEmail,
        String subject,
        Map<String, Object> variables
    ) {
        if (!mailProperties.enabled()) {
            return fallback(templateKey, recipientEmail, subject, variables, "메일 발송이 비활성화되어 로그 fallback으로 처리했습니다.");
        }

        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            return fallback(templateKey, recipientEmail, subject, variables, "JavaMailSender Bean이 없어 로그 fallback으로 처리했습니다.");
        }

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.fromEmail(), mailProperties.fromName());
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(render(templatePath, variables), true);
            javaMailSender.send(mimeMessage);
            return MailDeliveryResult.sent(templateKey, recipientEmail);
        } catch (Exception exception) {
            log.warn("메일 발송 실패: template={}, recipient={}", templateKey, recipientEmail, exception);
            if (mailProperties.fallbackToLog()) {
                return fallback(templateKey, recipientEmail, subject, variables, "메일 발송 실패로 로그 fallback 처리했습니다.");
            }
            throw new BusinessException(CommonErrorCode.EXTERNAL_API_ERROR, "메일 발송 중 오류가 발생했습니다.");
        }
    }

    private String render(String templatePath, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templatePath, context);
    }

    private MailDeliveryResult fallback(
        String templateKey,
        String recipientEmail,
        String subject,
        Map<String, Object> variables,
        String message
    ) {
        log.info(
            "MAIL_FALLBACK template={} recipient={} subject={} message={}",
            templateKey,
            recipientEmail,
            subject,
            message
        );
        return mailProperties.enabled()
            ? MailDeliveryResult.fallbackLogged(templateKey, recipientEmail, message)
            : MailDeliveryResult.skipped(templateKey, recipientEmail, message);
    }

    private String buildPasswordResetUrl(String resetCode) {
        return UriComponentsBuilder.fromUriString(buildFrontendUrl(mailProperties.passwordResetPath()))
            .queryParam("code", resetCode)
            .toUriString();
    }

    private String buildEmailVerificationUrl(String recipientEmail, String verificationCode) {
        return UriComponentsBuilder.fromUriString(buildFrontendUrl(mailProperties.emailVerificationPath()))
            .queryParam("email", recipientEmail)
            .queryParam("code", verificationCode)
            .toUriString();
    }

    private String buildFrontendUrl(String path) {
        return removeTrailingSlash(mailProperties.frontendBaseUrl()) + normalizePath(path);
    }

    private String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private String removeTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String defaultName(String name) {
        return name == null || name.isBlank() ? "금정야학 회원" : name;
    }
}
