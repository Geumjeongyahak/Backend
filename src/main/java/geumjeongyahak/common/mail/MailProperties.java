package geumjeongyahak.common.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
    boolean enabled,
    boolean fallbackToLog,
    String fromEmail,
    String fromName,
    String frontendBaseUrl,
    String passwordResetPath,
    long passwordResetExpirationMinutes,
    String emailVerificationPath,
    long emailVerificationExpirationMinutes
) {
    public MailProperties {
        if (fromEmail == null || fromEmail.isBlank()) {
            fromEmail = "no-reply@geumjeongschool.com";
        }
        if (fromName == null || fromName.isBlank()) {
            fromName = "금정야학";
        }
        if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
            frontendBaseUrl = "https://geumjeongschool.com";
        }
        if (passwordResetPath == null || passwordResetPath.isBlank()) {
            passwordResetPath = "/auth/reset-password";
        }
        if (passwordResetExpirationMinutes <= 0) {
            passwordResetExpirationMinutes = 15;
        }
        if (emailVerificationPath == null || emailVerificationPath.isBlank()) {
            emailVerificationPath = "/api/v1/auth/email-verification/confirm";
        }
        if (emailVerificationExpirationMinutes <= 0) {
            emailVerificationExpirationMinutes = 15;
        }
    }
}
