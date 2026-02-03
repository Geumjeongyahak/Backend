package sonmoeum.api.v1.users.dto.request;

import java.util.List;

import sonmoeum.common.validation.annotation.ValidPermissions;
import sonmoeum.common.validation.annotation.ValidPhoneNumber;
import sonmoeum.common.validation.annotation.ValidRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Schema(description = "이름", example = "홍길동")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "전화번호", example = "010-9876-5432")
    @ValidPhoneNumber
    String phoneNumber,

    @Schema(description = "비밀번호", example = "newpassword123!")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    String password,

    @Schema(description = "역할", example = "ADMIN")
    @ValidRole
    String role,

    @Schema(description = "권한 목록", example = "[\"MANAGE_USERS\", \"MANAGE_DEPARTMENTS\"]")
    @ValidPermissions
    List<String> permissions
) {

}
