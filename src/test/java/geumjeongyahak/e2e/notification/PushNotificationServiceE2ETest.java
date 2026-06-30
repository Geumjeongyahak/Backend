package geumjeongyahak.e2e.notification;

import geumjeongyahak.domain.notification.entity.PushSubscription;
import geumjeongyahak.domain.notification.enums.PushRequestType;
import geumjeongyahak.domain.notification.event.RequestReviewedPushEvent;
import geumjeongyahak.domain.notification.service.PushNotificationService;
import geumjeongyahak.domain.notification.service.PushSubscriptionService;
import geumjeongyahak.domain.notification.v1.dto.request.SubscribePushRequest;
import geumjeongyahak.domain.users.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;

import static geumjeongyahak.domain.notification.enums.PushDeviceType.WEB;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("E2E: Push 알림 발송 플로우 테스트")
@ResourceLock("notification-e2e-shared-state")
class PushNotificationServiceE2ETest extends BaseNotificationTest {

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private PushSubscriptionService pushSubscriptionService;

    @Test
    @DisplayName("요청 처리 알림 발송 성공 시 구독의 lastUsedAt이 갱신되고 실패 횟수가 초기화된다")
    void sendRequestReviewedPush_success_marksSubscriptionUsed() {
        User user = userTestHelper.getUser(TEST_NOTIFICATION_USER);
        pushSubscriptionServiceSubscribe(user.getId(), "fcm-token-success");

        pushNotificationService.sendRequestReviewedPush(RequestReviewedPushEvent.approved(
            user.getId(),
            10L,
            PushRequestType.ABSENCE,
            1L,
            "결석 요청 승인",
            "요청이 승인되었습니다.",
            "확인 완료"
        ));

        PushSubscription subscription = pushSubscriptionRepository.findByToken("fcm-token-success").orElseThrow();
        assertThat(subscription.isActive()).isTrue();
        assertThat(subscription.getLastUsedAt()).isNotNull();
        assertThat(subscription.getFailureCount()).isZero();
        assertThat(controlledPushNotificationSender.sentPushes()).hasSize(1);
        assertThat(controlledPushNotificationSender.sentPushes().getFirst().message().data())
            .containsEntry("eventType", "REQUEST_REVIEWED")
            .containsEntry("requestType", "ABSENCE")
            .containsEntry("reviewResult", "APPROVED")
            .containsEntry("note", "확인 완료");
    }

    @Test
    @DisplayName("유효하지 않은 토큰 발송 실패 시 구독을 비활성화하고 실패 횟수를 증가시킨다")
    void sendRequestReviewedPush_invalidToken_unsubscribesSubscription() {
        User user = userTestHelper.getUser(TEST_NOTIFICATION_USER);
        pushSubscriptionServiceSubscribe(user.getId(), "fcm-token-invalid");
        controlledPushNotificationSender.fail("fcm-token-invalid", true, "invalid token");

        pushNotificationService.sendRequestReviewedPush(RequestReviewedPushEvent.rejected(
            user.getId(),
            11L,
            PushRequestType.PURCHASE,
            1L,
            "구입 요청 반려",
            "요청이 반려되었습니다.",
            null
        ));

        PushSubscription subscription = pushSubscriptionRepository.findByToken("fcm-token-invalid").orElseThrow();
        assertThat(subscription.isActive()).isFalse();
        assertThat(subscription.getUnsubscribedAt()).isNotNull();
        assertThat(subscription.getFailureCount()).isEqualTo(1);
    }

    private void pushSubscriptionServiceSubscribe(Long userId, String token) {
        pushSubscriptionService.subscribe(userId, new SubscribePushRequest(token, WEB));
    }
}
