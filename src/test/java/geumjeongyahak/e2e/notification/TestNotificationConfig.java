package geumjeongyahak.e2e.notification;

import geumjeongyahak.domain.notification.dto.PushNotificationMessage;
import geumjeongyahak.domain.notification.dto.PushSendResult;
import geumjeongyahak.domain.notification.service.sender.PushNotificationSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class TestNotificationConfig {

    @Bean
    @Primary
    ControlledPushNotificationSender controlledPushNotificationSender() {
        return new ControlledPushNotificationSender();
    }

    public static class ControlledPushNotificationSender implements PushNotificationSender {
        private final Map<String, PushSendResult> resultsByToken = new HashMap<>();
        private final List<SentPush> sentPushes = new ArrayList<>();

        public void fail(String token, boolean invalidToken, String errorMessage) {
            resultsByToken.put(token, PushSendResult.failed(invalidToken, errorMessage));
        }

        public void reset() {
            resultsByToken.clear();
            sentPushes.clear();
        }

        public List<SentPush> sentPushes() {
            return List.copyOf(sentPushes);
        }

        @Override
        public PushSendResult send(String token, PushNotificationMessage message) {
            sentPushes.add(new SentPush(token, message));
            return resultsByToken.getOrDefault(token, PushSendResult.succeeded());
        }
    }

    public record SentPush(String token, PushNotificationMessage message) {
    }
}
