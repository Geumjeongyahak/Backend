package geumjeongyahak.domain.auth.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import geumjeongyahak.common.validation.annotation.ValidEmail;
import geumjeongyahak.common.validation.annotation.ValidPhoneNumber;

public record LocalSignupRequest(
        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @Schema(description = "이름", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @ValidEmail
        String email,

        @Schema(description = "전화번호", example = "010-1234-5678")
        @ValidPhoneNumber
        String phoneNumber
) {
}
