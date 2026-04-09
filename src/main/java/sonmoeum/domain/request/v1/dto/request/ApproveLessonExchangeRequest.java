package sonmoeum.domain.request.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ApproveLessonExchangeRequest(

    @NotNull
    @Schema(description = "교환할 교사(봉사자) ID", example = "2")
    Long exchangeWithUserId
) {}
