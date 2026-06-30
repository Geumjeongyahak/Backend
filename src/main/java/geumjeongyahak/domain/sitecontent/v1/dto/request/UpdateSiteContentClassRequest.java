package geumjeongyahak.domain.sitecontent.v1.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateSiteContentClassRequest(
    @NotBlank
    @Size(max = 80)
    String name,

    @JsonAlias("group")
    @NotBlank
    String groupId,

    List<@NotBlank String> description
) {
}
