package geumjeongyahak.domain.notification.service.sender;

import geumjeongyahak.domain.notification.dto.PushNotificationMessage;
import geumjeongyahak.domain.notification.dto.PushSendResult;

public interface PushNotificationSender {

    PushSendResult send(String token, PushNotificationMessage message);
}
