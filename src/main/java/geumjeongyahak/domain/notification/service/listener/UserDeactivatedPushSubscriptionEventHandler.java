package geumjeongyahak.domain.notification.service.listener;

import geumjeongyahak.domain.notification.repository.PushSubscriptionRepository;
import geumjeongyahak.domain.users.event.UserDeactivatedEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeactivatedPushSubscriptionEventHandler {

    private final PushSubscriptionRepository pushSubscriptionRepository;

    @EventListener
    public void handleUserDeactivated(UserDeactivatedEvent event) {
        int deactivatedCount = pushSubscriptionRepository.deactivateAllByUserId(
            event.userId(),
            LocalDateTime.now()
        );
        log.info(
            "비활성 사용자 Push 구독 해제 완료 - userId: {}, count: {}",
            event.userId(),
            deactivatedCount
        );
    }
}
