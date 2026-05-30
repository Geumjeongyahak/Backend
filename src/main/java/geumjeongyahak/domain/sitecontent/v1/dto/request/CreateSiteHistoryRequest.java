package geumjeongyahak.domain.sitecontent.v1.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateSiteHistoryRequest(
    @NotBlank
    @Size(max = 120)
    String title,

    String detail,

    @Size(max = 120)
    String linkLabel,

    String linkHref,

    List<@Valid SiteHistoryPhotoRequest> photos
) {
}
