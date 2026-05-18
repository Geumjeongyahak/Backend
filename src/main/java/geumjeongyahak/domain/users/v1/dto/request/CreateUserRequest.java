package geumjeongyahak.domain.users.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import geumjeongyahak.common.validation.annotation.ValidEmail;
import geumjeongyahak.common.validation.annotation.ValidPhoneNumber;

@Schema(description = "사용자 생성 요청 DTO. 사용자 기본 프로필과 Local 로그인 자격 증명을 함께 생성할 때 사용합니다.")
public record CreateUserRequest(
    @Schema(description = "Local 로그인 및 기본 연락처로 사용할 이메일 주소", example = "user@example.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @ValidEmail
    String email,

    @Schema(description = "사용자 실명 또는 운영상 관리 이름", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    String name,

    @Schema(description = "초기 Local 로그인 비밀번호", example = "password123!")
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    String password,

    @Schema(description = "연락 가능한 전화번호", example = "010-1234-5678")
    @ValidPhoneNumber
    String phoneNumber,

    @Schema(description = "사용자 기본 역할. 인가의 1차 기준이 되는 role 값입니다.", examples = { "ADMIN", "MANAGER", "VOLUNTEER", "GUEST" })
    String role,

    @Schema(description = "초기 소속 부서 ID. 없으면 null로 둘 수 있습니다.", example = "1")
    Long departmentId
) {

}
