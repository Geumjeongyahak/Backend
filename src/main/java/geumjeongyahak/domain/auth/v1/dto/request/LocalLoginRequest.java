package geumjeongyahak.domain.auth.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocalLoginRequest(
        @Schema(description = "사용자 아이디", example = "johndoe1234", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "ID는 필수입니다.")
        @Size(min = 8, message = "사용자 아이디는 8자 이상이어야 합니다.")
        String username,

        @Schema(description = "사용자 비밀번호", example = "P@ssw0rd!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password
) {
}
