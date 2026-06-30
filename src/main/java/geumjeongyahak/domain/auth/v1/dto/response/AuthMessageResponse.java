package geumjeongyahak.domain.auth.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 관련 메시지 응답")
public record AuthMessageResponse(
        @Schema(description = "응답 메시지", example = "로그아웃되었습니다.")
        String message
) {
    public static AuthMessageResponse of(String message) {
        return new AuthMessageResponse(message);
    }
}
