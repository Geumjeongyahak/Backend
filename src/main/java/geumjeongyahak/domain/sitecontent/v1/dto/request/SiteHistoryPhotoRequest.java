package geumjeongyahak.domain.sitecontent.v1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SiteHistoryPhotoRequest(
    Long id,

    UUID fileId,

    @NotBlank
    String src,

    @Size(max = 255)
    String alt
) {
}
