package geumjeongyahak.domain.sitecontent.v1.dto.response;

import java.util.List;

public record SiteHistoriesResponse(
    List<SiteHistoryResponse> history
) {
}
