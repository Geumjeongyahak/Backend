package geumjeongyahak.domain.users.v1.dto.request;

import geumjeongyahak.common.validation.annotation.ValidPhoneNumber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import geumjeongyahak.common.validation.annotation.ValidRole;

@Schema(description = "관리자/운영자용 사용자 정보 수정 요청 DTO. 전달한 필드만 반영합니다.")
public record UpdateUserRequest(
    @Schema(description = "변경할 이름", example = "홍길동")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "변경할 전화번호", example = "010-9876-5432")
    @ValidPhoneNumber
    String phoneNumber,

    @Schema(description = "변경할 이메일 주소. 사용자 기본 이메일과 Local credential 이메일이 함께 갱신될 수 있습니다.", example = "example@domain.com")
    String email,

    @Schema(description = "변경할 Local 로그인 비밀번호", example = "newpassword123!")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    String password,

    @Schema(description = "변경할 기본 역할", examples = { "ADMIN", "MANAGER", "VOLUNTEER", "GUEST" })
    @ValidRole
    String role,

    @Schema(description = "변경할 소속 부서 ID", example = "1")
    Long departmentId,

    @Schema(description = "변경할 배정 분반 ID", example = "1")
    Long classroomId
) {
    public UpdateUserRequest(
        String name,
        String phoneNumber,
        String email,
        String password,
        String role,
        Long departmentId
    ) {
        this(name, phoneNumber, email, password, role, departmentId, null);
    }
}
