package geumjeongyahak.domain.sitecontent.v1.dto.response;

import java.util.List;

public record SiteContentClassesResponse(
    List<SiteContentClassResponse> weekday,
    List<SiteContentClassResponse> weekendMorning,
    List<SiteContentClassResponse> weekendAfternoon
) {
}
