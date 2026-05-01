package geumjeongyahak.domain.auth.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
    @Schema(description = "콜백에서 발급된 임시 토큰")
    @NotBlank String tempToken
) {}
