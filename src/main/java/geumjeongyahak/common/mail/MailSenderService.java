package geumjeongyahak.common.mail;

public interface MailSenderService {

    MailDeliveryResult sendSignupWelcomeMail(String recipientEmail, String recipientName);

    MailDeliveryResult sendPasswordResetMail(String recipientEmail, String recipientName, String resetCode);

    MailDeliveryResult sendEmailVerificationMail(String recipientEmail, String recipientName, String verificationCode);
}
