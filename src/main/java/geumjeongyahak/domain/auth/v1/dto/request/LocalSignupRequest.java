package geumjeongyahak.domain.auth.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import geumjeongyahak.common.validation.annotation.ValidEmail;
import geumjeongyahak.common.validation.annotation.ValidPhoneNumber;

public record LocalSignupRequest(
        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @Schema(description = "닉네임", example = "홍길동")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
        String nickname,

        @Schema(description = "이름", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @ValidEmail
        String email,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String profileImageUrl,

        @Schema(description = "전화번호", example = "010-1234-5678")
        @ValidPhoneNumber
        String phoneNumber,

        @Schema(description = "주민등록번호 앞자리", example = "900101")
        @NotBlank(message = "주민등록번호 앞자리는 필수입니다.")
        @Pattern(regexp = "\\d{6}", message = "주민등록번호 앞자리는 숫자 6자리여야 합니다.")
        String residentRegistrationNumberPrefix
) {
}
