package sonmoeum.api.v1.auth.dto.request;

import sonmoeum.common.validation.annotation.ValidEmail;
import sonmoeum.common.validation.annotation.ValidRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailSignupRequest(
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    String name,

    @NotBlank(message = "이메일은 필수입니다.")
    @ValidEmail
    String email,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    String password,

    String phoneNumber,

    @NotBlank(message = "역할은 필수입니다.")
    @ValidRole
    String role
) {}
