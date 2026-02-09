package sonmoeum.domain.auth.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그아웃 요청")
public record LogoutRequest(
        @NotBlank(message = "Refresh Token은 필수입니다.")
        @Schema(description = "Refresh Token", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken
) {
}
