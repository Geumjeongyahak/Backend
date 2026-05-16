package geumjeongyahak.domain.notification.v1.dto.request;

import geumjeongyahak.domain.notification.enums.PushDeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Push 알림 구독 요청 DTO입니다. 클라이언트가 발급받은 FCM 토큰을 현재 로그인한 사용자와 연결합니다.")
public record SubscribePushRequest(
    @Schema(
        description = "Firebase Cloud Messaging에서 발급받은 기기 토큰입니다.",
        example = "fcm-token"
    )
    @NotBlank(message = "FCM 토큰은 필수입니다.")
    @Size(max = 1024, message = "FCM 토큰은 1024자 이하로 입력해주세요.")
    String token,

    @Schema(
        description = "토큰이 발급된 클라이언트 기기 유형입니다. WEB, ANDROID, IOS 중 하나입니다.",
        example = "WEB"
    )
    @NotNull(message = "기기 유형은 필수입니다.")
    PushDeviceType deviceType
) {
}
