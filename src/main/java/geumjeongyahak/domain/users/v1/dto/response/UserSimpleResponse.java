package geumjeongyahak.domain.users.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import geumjeongyahak.domain.users.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 목록 응답용 기본 정보 DTO. 상세 권한 목록과 생성/수정 시각은 포함하지 않습니다.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserSimpleResponse(
    @Schema(description = "사용자 식별자", example = "1")
    Long id,

    @Schema(description = "사용자 이름", example = "홍길동")
    String name,

    @Schema(description = "사용자 기본 이메일", example = "user@example.com")
    String email,

    @Schema(description = "연락 가능한 전화번호", example = "010-1234-5678")
    String phoneNumber,

    @Schema(description = "사용자 기본 역할(role)", examples = { "ADMIN", "MANAGER", "VOLUNTEER", "GUEST" })
    String role,

    @Schema(description = "소속 부서 ID. 소속 부서가 없으면 null일 수 있습니다.", example = "2", nullable = true)
    Long departmentId
) {
    public static UserSimpleResponse from(User user) {
        return new UserSimpleResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getRole().name(),
            user.getDepartment() != null ? user.getDepartment().getId() : null
        );
    }
}
