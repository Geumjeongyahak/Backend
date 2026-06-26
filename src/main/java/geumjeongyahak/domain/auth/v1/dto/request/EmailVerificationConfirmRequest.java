package geumjeongyahak.domain.auth.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationConfirmRequest(
    @Schema(description = "인증할 Local 로그인 이메일", example = "user@example.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @ValidEmail
    String email,

    @Schema(description = "메일로 받은 6자리 이메일 인증번호", example = "123456")
    @NotBlank(message = "인증번호는 필수입니다.")
    @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자여야 합니다.")
    String verificationCode
) {
}
