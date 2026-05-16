package geumjeongyahak.domain.notification.service.sender;

import geumjeongyahak.domain.notification.dto.PushNotificationMessage;
import geumjeongyahak.domain.notification.dto.PushSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnMissingBean(PushNotificationSender.class)
public class NoOpPushNotificationSender implements PushNotificationSender {

    @Override
    public PushSendResult send(String token, PushNotificationMessage message) {
        log.info("Firebase 비활성화 상태로 Push 발송을 건너뜁니다. (title={})", message.title());
        return PushSendResult.succeeded();
    }
}
