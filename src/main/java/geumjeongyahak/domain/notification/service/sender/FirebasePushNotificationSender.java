package geumjeongyahak.domain.notification.service.sender;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import geumjeongyahak.domain.notification.dto.PushNotificationMessage;
import geumjeongyahak.domain.notification.dto.PushSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(FirebaseMessaging.class)
public class FirebasePushNotificationSender implements PushNotificationSender {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public PushSendResult send(String token, PushNotificationMessage message) {
        Message firebaseMessage = Message.builder()
            .setToken(token)
            .setNotification(Notification.builder()
                .setTitle(message.title())
                .setBody(message.body())
                .build()
            )
            .putAllData(message.data())
            .build();

        try {
            firebaseMessaging.send(firebaseMessage);
            return PushSendResult.succeeded();
        } catch (FirebaseMessagingException exception) {
            boolean invalidToken = isInvalidToken(exception);
            log.warn(
                "Firebase Push 발송 실패 (invalidToken={}, errorCode={}, message={})",
                invalidToken,
                exception.getMessagingErrorCode(),
                exception.getMessage()
            );
            return PushSendResult.failed(invalidToken, exception.getMessage());
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException exception) {
        MessagingErrorCode code = exception.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED
            || code == MessagingErrorCode.INVALID_ARGUMENT;
    }
}
