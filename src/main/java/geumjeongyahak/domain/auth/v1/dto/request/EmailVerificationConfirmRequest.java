package geumjeongyahak.domain.auth.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationConfirmRequest(
    @Schema(description = "인증할 Local 로그인 이메일", example = "user@example.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @ValidEmail
    String email,

    @Schema(description = "메일 인증 token", example = "0iWz3Xc7rC7m6R8uW4ME7jnhEtvXjqUy5S9Gc1hxmKw")
    @NotBlank(message = "인증번호는 필수입니다.")
    String verificationCode
) {
}
