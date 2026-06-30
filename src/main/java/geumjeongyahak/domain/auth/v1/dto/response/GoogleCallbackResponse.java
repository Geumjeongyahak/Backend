package geumjeongyahak.domain.auth.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Google OAuth 콜백 응답 (팝업→프론트 전달용)")
public record GoogleCallbackResponse(
    @Schema(description = "임시 토큰 (로그인/회원가입에 사용)")
    String tempToken,

    @Schema(description = "회원가입 필요 여부")
    boolean signupRequired,

    @Schema(description = "Local 계정과 연동 여부")
    boolean connectedToLocal,

    @Schema(description = "Google 이메일")
    String email,

    @Schema(description = "Google 이름")
    String name,

    @Schema(description = "Google 프로필 이미지 URL")
    String profileImageUrl
) {}
