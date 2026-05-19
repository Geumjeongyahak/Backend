package geumjeongyahak.domain.notification.service.listener;

import geumjeongyahak.domain.notification.event.PurchaseStatusChangedPushEvent;
import geumjeongyahak.domain.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseStatusChangedPushEventHandler {

    private final PushNotificationService pushNotificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PurchaseStatusChangedPushEvent event) {
        log.info(
            "구입 요청 상태 변경 Push 알림 이벤트 수신 (userId={}, requestId={}, newStatus={})",
            event.getUserId(),
            event.getRequestId(),
            event.getNewStatus()
        );
        pushNotificationService.sendPurchaseStatusChangedPush(event);
    }
}
