package geumjeongyahak.domain.sitecontent.v1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SiteHistoryLinkRequest(
    @NotBlank
    @Size(max = 120)
    String label,

    @NotBlank
    String href
) {
}
