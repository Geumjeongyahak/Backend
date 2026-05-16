package geumjeongyahak.domain.notification.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.notification.service.PushSubscriptionService;
import geumjeongyahak.domain.notification.v1.dto.request.SubscribePushRequest;
import geumjeongyahak.domain.notification.v1.dto.response.PushSubscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/push/subscriptions")
@Tag(
    name = "Push Subscription",
    description = "Firebase Cloud Messaging 토큰을 현재 로그인한 사용자와 연결하거나 해지하는 API입니다."
)
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Push 알림 구독",
        description = "클라이언트가 Firebase에서 발급받은 FCM 토큰을 현재 로그인한 사용자에게 연결합니다. "
            + "동일 토큰이 이미 존재하면 새 레코드를 만들지 않고 기존 구독을 재활성화합니다."
    )
    @PostMapping
    public ResponseEntity<PushSubscriptionResponse> subscribe(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody SubscribePushRequest request
    ) {
        log.debug("POST /api/v1/push/subscriptions - Push 구독 요청 (userId={})", userDetails.getUserId());
        PushSubscriptionResponse response = pushSubscriptionService.subscribe(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Push 알림 구독 해지",
        description = "현재 로그인한 사용자의 Push 구독을 비활성화합니다. "
            + "구독 레코드는 물리 삭제하지 않고 active=false 상태로 전환합니다."
    )
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> unsubscribe(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Parameter(description = "해지할 Push 구독 ID입니다.", example = "1")
        @PathVariable Long subscriptionId
    ) {
        log.debug(
            "DELETE /api/v1/push/subscriptions/{} - Push 구독 해지 요청 (userId={})",
            subscriptionId,
            userDetails.getUserId()
        );
        pushSubscriptionService.unsubscribe(userDetails.getUserId(), subscriptionId);
        return ResponseEntity.noContent().build();
    }
}
