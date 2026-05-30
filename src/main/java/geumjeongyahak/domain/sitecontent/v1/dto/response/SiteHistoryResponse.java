package geumjeongyahak.domain.sitecontent.v1.dto.response;

import geumjeongyahak.domain.sitecontent.entity.SiteHistory;
import geumjeongyahak.domain.sitecontent.entity.SiteHistoryPhoto;
import java.util.Comparator;
import java.util.List;

public record SiteHistoryResponse(
    Long id,
    String title,
    String detail,
    String linkLabel,
    String linkHref,
    List<SiteHistoryPhotoResponse> photos
) {
    public static SiteHistoryResponse from(SiteHistory history) {
        return new SiteHistoryResponse(
            history.getId(),
            history.getTitle(),
            history.getDetail(),
            history.getLinkLabel(),
            history.getLinkHref(),
            history.getPhotos().stream()
                .sorted(Comparator
                    .comparingInt(SiteHistoryPhoto::getSortOrder)
                    .thenComparing(SiteHistoryPhoto::getId))
                .map(SiteHistoryPhotoResponse::from)
                .toList()
        );
    }
}
