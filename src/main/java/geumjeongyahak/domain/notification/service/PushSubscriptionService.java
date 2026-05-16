package geumjeongyahak.domain.notification.service;

import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.domain.notification.entity.PushSubscription;
import geumjeongyahak.domain.notification.repository.PushSubscriptionRepository;
import geumjeongyahak.domain.notification.v1.dto.request.SubscribePushRequest;
import geumjeongyahak.domain.notification.v1.dto.response.PushSubscriptionResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserProxyService userProxyService;

    @Transactional
    public PushSubscriptionResponse subscribe(Long userId, SubscribePushRequest request) {
        String token = normalizeToken(request.token());
        User user = userProxyService.getReferenceById(userId);

        PushSubscription subscription = pushSubscriptionRepository.findByToken(token)
            .map(existing -> {
                existing.resubscribe(user, request.deviceType());
                return existing;
            })
            .orElseGet(() -> new PushSubscription(user, token, request.deviceType()));

        PushSubscription saved = pushSubscriptionRepository.save(subscription);
        log.debug("Push 구독 저장 완료 (subscriptionId={}, userId={})", saved.getId(), userId);
        return PushSubscriptionResponse.from(saved);
    }

    @Transactional
    public void unsubscribe(Long userId, Long subscriptionId) {
        PushSubscription subscription = pushSubscriptionRepository.findByIdAndUserId(subscriptionId, userId)
            .orElseThrow(() -> new BadRequestException(CommonErrorCode.RESOURCE_NOT_FOUND));
        subscription.unsubscribe();
        log.debug("Push 구독 해지 완료 (subscriptionId={}, userId={})", subscriptionId, userId);
    }

    @Transactional(readOnly = true)
    public List<PushSubscription> getActiveSubscriptions(Long userId) {
        return pushSubscriptionRepository.findAllByUserIdAndActiveTrue(userId);
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException(CommonErrorCode.MISSING_REQUIRED_FIELD, "FCM 토큰은 필수입니다.");
        }
        return token.trim();
    }
}
