package geumjeongyahak.unit.notification;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.notification.repository.PushSubscriptionRepository;
import geumjeongyahak.domain.notification.service.listener.UserDeactivatedPushSubscriptionEventHandler;
import geumjeongyahak.domain.users.event.UserDeactivatedEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDeactivatedPushSubscriptionEventHandlerTest {

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;

    @InjectMocks
    private UserDeactivatedPushSubscriptionEventHandler eventHandler;

    @Test
    void handleUserDeactivated_deactivatesAllActivePushSubscriptions() {
        Long userId = 10L;

        eventHandler.handleUserDeactivated(new UserDeactivatedEvent(userId));

        ArgumentCaptor<LocalDateTime> unsubscribedAtCaptor =
            ArgumentCaptor.forClass(LocalDateTime.class);
        verify(pushSubscriptionRepository).deactivateAllByUserId(
            eq(userId),
            unsubscribedAtCaptor.capture()
        );
    }
}
