package sonmoeum.api.v1.auth.dto.request;

import sonmoeum.common.validation.annotation.ValidEmail;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record EmailLoginRequest(
    @Schema(description = "이메일", example = "teacher@test.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @ValidEmail
    String email,

    @Schema(description = "비밀번호", example = "teacher")
    @NotBlank(message = "비밀번호는 필수입니다.")
    String password
) {}
