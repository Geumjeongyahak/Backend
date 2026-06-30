package geumjeongyahak.common.mail;

public record MailDeliveryResult(
    MailDeliveryStatus status,
    String template,
    String recipient,
    String message
) {
    public static MailDeliveryResult sent(String template, String recipient) {
        return new MailDeliveryResult(MailDeliveryStatus.SENT, template, recipient, "메일이 발송되었습니다.");
    }

    public static MailDeliveryResult skipped(String template, String recipient, String message) {
        return new MailDeliveryResult(MailDeliveryStatus.SKIPPED, template, recipient, message);
    }

    public static MailDeliveryResult fallbackLogged(String template, String recipient, String message) {
        return new MailDeliveryResult(MailDeliveryStatus.FALLBACK_LOGGED, template, recipient, message);
    }
}
