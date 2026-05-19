package geumjeongyahak.domain.notification.service.listener;

import geumjeongyahak.domain.notification.event.RequestReviewedPushEvent;
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
public class RequestReviewedPushEventHandler {

    private final PushNotificationService pushNotificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RequestReviewedPushEvent event) {
        log.info(
            "요청 처리 Push 알림 이벤트 수신 (userId={}, requestType={}, requestId={}, reviewResult={})",
            event.getUserId(),
            event.getRequestType(),
            event.getRequestId(),
            event.getReviewResult()
        );
        pushNotificationService.sendRequestReviewedPush(event);
    }
}
