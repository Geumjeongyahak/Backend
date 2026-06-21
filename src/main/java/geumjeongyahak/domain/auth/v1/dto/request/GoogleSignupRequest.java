package geumjeongyahak.domain.auth.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import geumjeongyahak.common.validation.annotation.ValidPhoneNumber;
import geumjeongyahak.common.validation.annotation.ValidUserBirthDate;
import java.time.LocalDate;

public record GoogleSignupRequest(
    @Schema(description = "콜백에서 발급된 임시 토큰")
    @NotBlank String tempToken,

    @Schema(description = "이름", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "전화번호", example = "010-1234-5678")
    @ValidPhoneNumber String phoneNumber,

    @Schema(description = "생년월일", example = "1990-01-01")
    @NotNull(message = "생년월일은 필수입니다.")
    @ValidUserBirthDate
    LocalDate birthDate
) {}
