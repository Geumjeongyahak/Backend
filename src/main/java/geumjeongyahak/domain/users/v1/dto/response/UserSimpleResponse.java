package geumjeongyahak.domain.users.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import geumjeongyahak.domain.users.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 기본 정보")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserSimpleResponse(
    @Schema(description = "사용자 식별자", example = "1")
    Long id,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "닉네임", example = "까치")
    String nickname,

    @Schema(description = "이메일", example = "user@example.com")
    String email,

    @Schema(description = "전화번호", example = "010-1234-5678")
    String phoneNumber,

    @Schema(description = "역할", examples = { "ADMIN", "MANAGER", "VOLUNTEER", "GUEST" })
    String role,

    @Schema(description = "부서 ID", example = "2")
    Long departmentId
) {
    public static UserSimpleResponse from(User user) {
        return new UserSimpleResponse(
            user.getId(),
            user.getName(),
            user.getNickname(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getRole().name(),
            user.getDepartment() != null ? user.getDepartment().getId() : null
        );
    }
}
