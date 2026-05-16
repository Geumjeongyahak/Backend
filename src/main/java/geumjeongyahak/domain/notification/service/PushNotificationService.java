package geumjeongyahak.domain.notification.service;

import geumjeongyahak.domain.notification.dto.PushNotificationMessage;
import geumjeongyahak.domain.notification.dto.PushSendResult;
import geumjeongyahak.domain.notification.entity.PushSubscription;
import geumjeongyahak.domain.notification.event.PurchaseStatusChangedPushEvent;
import geumjeongyahak.domain.notification.event.RequestReviewedPushEvent;
import geumjeongyahak.domain.notification.repository.PushSubscriptionRepository;
import geumjeongyahak.domain.notification.service.sender.PushNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PushNotificationSender pushNotificationSender;

    @Transactional
    public void sendRequestReviewedPush(RequestReviewedPushEvent event) {
        List<PushSubscription> subscriptions =
            pushSubscriptionRepository.findAllByUserIdAndActiveTrue(event.getUserId());

        if (subscriptions.isEmpty()) {
            log.info("활성 Push 구독이 없어 알림을 발송하지 않습니다. (userId={})", event.getUserId());
            return;
        }

        PushNotificationMessage message = PushNotificationMessage.from(event);
        subscriptions.forEach(subscription -> send(subscription, message));
    }

    @Transactional
    public void sendPurchaseStatusChangedPush(PurchaseStatusChangedPushEvent event) {
        List<PushSubscription> subscriptions =
            pushSubscriptionRepository.findAllByUserIdAndActiveTrue(event.getUserId());

        if (subscriptions.isEmpty()) {
            log.info("활성 Push 구독이 없어 알림을 발송하지 않습니다. (userId={})", event.getUserId());
            return;
        }

        PushNotificationMessage message = PushNotificationMessage.from(event);
        subscriptions.forEach(subscription -> send(subscription, message));
    }

    private void send(PushSubscription subscription, PushNotificationMessage message) {
        PushSendResult result = pushNotificationSender.send(subscription.getToken(), message);

        if (result.success()) {
            subscription.markUsed();
            return;
        }

        subscription.markFailure();
        if (result.invalidToken()) {
            subscription.unsubscribe();
        }
    }
}
