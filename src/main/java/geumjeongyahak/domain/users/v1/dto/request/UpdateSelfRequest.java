package geumjeongyahak.domain.users.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import geumjeongyahak.common.validation.annotation.ValidPhoneNumber;
import geumjeongyahak.common.validation.annotation.ValidUserBirthDate;
import java.time.LocalDate;

@Schema(description = "사용자 본인 정보 수정 요청 DTO. role과 department는 수정 대상이 아닙니다.")
public record UpdateSelfRequest(
        @Schema(description = "변경할 이름", example = "홍길동")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "변경할 전화번호", example = "010-9876-5432")
        @ValidPhoneNumber
        String phoneNumber,

        @Schema(description = "변경할 생년월일", example = "1990-01-01")
        @ValidUserBirthDate
        LocalDate birthDate,

        @Schema(description = "변경할 이메일 주소. 사용자 기본 이메일과 Local credential 이메일이 함께 갱신될 수 있습니다.", example = "example@domain.com")
        String email,

        @Schema(description = "변경할 Local 로그인 비밀번호", example = "newpassword123!")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password
) {
}
