package sonmoeum.domain.auth.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh Token 재발급 요청")
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh Token은 필수입니다.")
        @Schema(description = "Refresh Token", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken
) {
}
