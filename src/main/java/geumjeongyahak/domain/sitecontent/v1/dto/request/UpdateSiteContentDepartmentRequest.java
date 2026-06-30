package geumjeongyahak.domain.sitecontent.v1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateSiteContentDepartmentRequest(
    @NotBlank
    @Size(max = 120)
    String title,

    @Size(max = 80)
    String name,

    List<@NotBlank String> responsibilities
) {
}
