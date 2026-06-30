package geumjeongyahak.domain.auth.service.event;

import geumjeongyahak.common.mail.MailSenderService;
import geumjeongyahak.domain.auth.event.UserSignedUpEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSignedUpEventHandler {

    private final MailSenderService mailSenderService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserSignedUp(UserSignedUpEvent event) {
        try {
            mailSenderService.sendSignupWelcomeMail(event.email(), event.name());
        } catch (Exception exception) {
            log.warn("회원가입 환영 메일 발송 실패 - userId={}, email={}", event.userId(), event.email(), exception);
        }
    }
}
