package sonmoeum.domain.users.v1.dto.request;

import sonmoeum.common.validation.annotation.ValidPhoneNumber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import sonmoeum.common.validation.annotation.ValidRole;

public record UpdateUserRequest(
    @Schema(description = "이름", example = "홍길동")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "전화번호", example = "010-9876-5432")
    @ValidPhoneNumber
    String phoneNumber,

    @Schema(description = "연락 가능한 이메일 주소", example = "example@domain.com")
    String email,

    @Schema(description = "비밀번호", example = "newpassword123!")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    String password,

    @Schema(description = "기본 역할", examples = { "ADMIN", "MANAGER", "VOLUNTEER", "GUEST" })
    @ValidRole(levels = { 0 })
    String role
) {
}
