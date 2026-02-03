package sonmoeum.api.v1.users.dto.response;

import java.util.List;

import sonmoeum.domain.users.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
    @Schema(description = "사용자 ID", example = "1")
    Long id,
    @Schema(description = "이름", example = "홍길동")
    String name,
    @Schema(description = "이메일", example = "user@example.com")
    String email,
    @Schema(description = "전화번호", example = "010-1234-5678")
    String phoneNumber,
    @Schema(description = "역할", example = "VOLUNTEER")
    String role,
    @Schema(description = "권한 목록", example = "[\"MANAGE_USERS\"]")
    List<String> permissions
) {
    
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getRole().name(),
            user.getPermissions().stream()
                .map(Enum::name)
                .toList()
        );
    }
}
