package geumjeongyahak.domain.auth.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
    @Schema(description = "Local 로그인 이메일", example = "user@example.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @ValidEmail
    String email,

    @Schema(description = "메일로 받은 6자리 인증번호", example = "123456")
    @NotBlank(message = "인증번호는 필수입니다.")
    String resetCode,

    @Schema(description = "새 비밀번호", example = "newpassword123!")
    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    String newPassword
) {
}
