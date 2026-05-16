package geumjeongyahak.domain.notification.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.notification.config.FirebaseProperties;
import geumjeongyahak.domain.notification.service.PushSubscriptionService;
import geumjeongyahak.domain.notification.v1.dto.request.SubscribePushRequest;
import geumjeongyahak.domain.notification.v1.dto.response.PushSubscriptionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/push")
public class PushSubscriptionViewController {

    private final PushSubscriptionService pushSubscriptionService;
    private final FirebaseProperties firebaseProperties;

    @GetMapping("/config")
    public AdminPushConfigResponse config() {
        return new AdminPushConfigResponse(
            isWebPushConfigured(),
            firebaseProperties.webApiKey(),
            firebaseProperties.webAuthDomain(),
            firebaseProperties.webProjectId(),
            firebaseProperties.webStorageBucket(),
            firebaseProperties.webMessagingSenderId(),
            firebaseProperties.webAppId(),
            firebaseProperties.webVapidKey()
        );
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<PushSubscriptionResponse> subscribe(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody SubscribePushRequest request
    ) {
        return ResponseEntity.ok(pushSubscriptionService.subscribe(userDetails.getUserId(), request));
    }

    @DeleteMapping("/subscriptions/{subscriptionId}")
    public ResponseEntity<Void> unsubscribe(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long subscriptionId
    ) {
        pushSubscriptionService.unsubscribe(userDetails.getUserId(), subscriptionId);
        return ResponseEntity.noContent().build();
    }

    private boolean isWebPushConfigured() {
        return firebaseProperties.enabled()
            && StringUtils.hasText(firebaseProperties.webApiKey())
            && StringUtils.hasText(firebaseProperties.webProjectId())
            && StringUtils.hasText(firebaseProperties.webMessagingSenderId())
            && StringUtils.hasText(firebaseProperties.webAppId())
            && StringUtils.hasText(firebaseProperties.webVapidKey());
    }

    public record AdminPushConfigResponse(
        boolean enabled,
        String apiKey,
        String authDomain,
        String projectId,
        String storageBucket,
        String messagingSenderId,
        String appId,
        String vapidKey
    ) {
    }
}
