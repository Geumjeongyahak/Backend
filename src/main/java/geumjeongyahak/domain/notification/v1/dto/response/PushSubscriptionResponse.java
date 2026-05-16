package geumjeongyahak.domain.notification.v1.dto.response;

import geumjeongyahak.domain.notification.entity.PushSubscription;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Push 알림 구독 응답 DTO입니다.")
public record PushSubscriptionResponse(
    @Schema(description = "Push 구독 ID입니다.", example = "1")
    Long id,

    @Schema(description = "구독이 연결된 사용자 ID입니다.", example = "10")
    Long userId,

    @Schema(description = "기기 유형입니다.", example = "WEB")
    String deviceType,

    @Schema(description = "현재 활성 구독 여부입니다.", example = "true")
    boolean active,

    @Schema(description = "구독 시각입니다.", example = "2026-05-16T19:30:00")
    LocalDateTime subscribedAt,

    @Schema(description = "구독 해지 시각입니다. 활성 구독이면 null입니다.", nullable = true)
    LocalDateTime unsubscribedAt,

    @Schema(description = "마지막 발송 성공 시각입니다. 아직 발송 성공 이력이 없으면 null입니다.", nullable = true)
    LocalDateTime lastUsedAt,

    @Schema(description = "연속 발송 실패 횟수입니다.", example = "0")
    int failureCount
) {
    public static PushSubscriptionResponse from(PushSubscription subscription) {
        return new PushSubscriptionResponse(
            subscription.getId(),
            subscription.getUser().getId(),
            subscription.getDeviceType().name(),
            subscription.isActive(),
            subscription.getSubscribedAt(),
            subscription.getUnsubscribedAt(),
            subscription.getLastUsedAt(),
            subscription.getFailureCount()
        );
    }
}
