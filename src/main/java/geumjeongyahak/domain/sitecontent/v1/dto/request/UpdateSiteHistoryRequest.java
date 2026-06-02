package geumjeongyahak.domain.sitecontent.v1.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateSiteHistoryRequest(
    @NotBlank
    @Size(max = 120)
    String title,

    @NotNull
    LocalDate historyDate,

    String detail,

    @Null
    @JsonProperty(value = "linkLabel", access = JsonProperty.Access.WRITE_ONLY)
    @Schema(hidden = true)
    String deprecatedLinkLabel,

    @Null
    @JsonProperty(value = "linkHref", access = JsonProperty.Access.WRITE_ONLY)
    @Schema(hidden = true)
    String deprecatedLinkHref,

    List<@Valid SiteHistoryLinkRequest> links,

    List<@Valid SiteHistoryPhotoRequest> photos
) {
}
