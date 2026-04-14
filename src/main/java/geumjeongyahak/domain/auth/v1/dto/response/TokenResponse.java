package geumjeongyahak.domain.auth.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "JWT 토큰 응답")
public record TokenResponse(
        @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Refresh Token", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String refreshToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 만료 시각", example = "2026-02-10T15:30:00")
        LocalDateTime accessTokenExpiresAt,

        @Schema(description = "Refresh Token 만료 시각", example = "2026-02-24T14:30:00")
        LocalDateTime refreshTokenExpiresAt
) {
    public static TokenResponse of(
            String accessToken,
            String refreshToken,
            LocalDateTime accessTokenExpiresAt,
            LocalDateTime refreshTokenExpiresAt
    ) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", accessTokenExpiresAt, refreshTokenExpiresAt);
    }
}
