package geumjeongyahak.domain.auth.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationResendRequest(
    @Schema(description = "인증 메일을 다시 받을 Local 로그인 이메일", example = "user@example.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @ValidEmail
    String email
) {
}
