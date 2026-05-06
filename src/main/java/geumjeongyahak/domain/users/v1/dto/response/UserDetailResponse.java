package geumjeongyahak.domain.users.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import geumjeongyahak.domain.auth.v1.dto.response.PermissionResponse;
import geumjeongyahak.domain.users.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "사용자 상세 응답 DTO. 관리자 상세 화면과 본인 정보 조회 응답에 공통으로 사용합니다.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDetailResponse(
    @Schema(description = "사용자 식별자", example = "1")
    Long id,

    @Schema(description = "사용자 이름", example = "홍길동")
    String name,

    @Schema(description = "서비스 내 표시용 닉네임", example = "까치")
    String nickname,

    @Schema(description = "사용자 기본 이메일이자 Local 로그인 이메일로 사용될 수 있는 값", example = "user@example.com")
    String email,

    @Schema(description = "연락 가능한 전화번호", example = "010-1234-5678")
    String phoneNumber,

    @Schema(description = "사용자 기본 역할(role)", examples = { "ADMIN", "MANAGER", "VOLUNTEER", "GUEST" })
    String role,

    @Schema(description = "소속 부서 ID. 소속 부서가 없으면 null일 수 있습니다.", example = "2", nullable = true)
    Long departmentId,

    @Schema(description = "사용자에게 직접 부여된 authority 목록. role 기반 권한이나 부서 권한은 포함하지 않습니다.")
    List<PermissionResponse> permissions,

    @Schema(description = "사용자 계정 생성 일시", example = "2024-01-01T12:00:00")
    LocalDateTime createdAt,

    @Schema(description = "사용자 기본 정보 마지막 수정 일시", example = "2024-01-02T15:30:00")
    LocalDateTime updatedAt
) {
    public static UserDetailResponse from(User user) {
        return new UserDetailResponse(
            user.getId(),
            user.getName(),
            user.getNickname(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getRole().name(),
            user.getDepartment() != null ? user.getDepartment().getId() : null,
            user.getPermissions().stream()
                .map(permission -> new PermissionResponse(
                    permission.toAuthorityCode(),
                    permission.toAuthorityCode()
                ))
                .toList(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
