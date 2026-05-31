package geumjeongyahak.domain.sitecontent.v1.dto.response;

import geumjeongyahak.domain.sitecontent.entity.SiteHistory;
import geumjeongyahak.domain.sitecontent.entity.SiteHistoryLink;
import geumjeongyahak.domain.sitecontent.entity.SiteHistoryPhoto;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record SiteHistoryResponse(
    Long id,
    String title,
    LocalDate historyDate,
    String detail,
    List<SiteHistoryLinkResponse> links,
    List<SiteHistoryPhotoResponse> photos
) {
    public static SiteHistoryResponse from(SiteHistory history) {
        List<SiteHistoryLinkResponse> links = history.getLinks().stream()
            .sorted(Comparator
                .comparingInt(SiteHistoryLink::getSortOrder)
            .thenComparing(SiteHistoryLink::getId))
            .map(SiteHistoryLinkResponse::from)
            .toList();
        return new SiteHistoryResponse(
            history.getId(),
            history.getTitle(),
            history.getHistoryDate(),
            history.getDetail(),
            links,
            history.getPhotos().stream()
                .sorted(Comparator
                    .comparingInt(SiteHistoryPhoto::getSortOrder)
                    .thenComparing(SiteHistoryPhoto::getId))
                .map(SiteHistoryPhotoResponse::from)
                .toList()
        );
    }
}
