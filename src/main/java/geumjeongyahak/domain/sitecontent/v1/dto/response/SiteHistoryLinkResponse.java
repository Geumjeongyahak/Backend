package geumjeongyahak.domain.sitecontent.v1.dto.response;

import geumjeongyahak.domain.sitecontent.entity.SiteHistoryLink;

public record SiteHistoryLinkResponse(
    Long id,
    String label,
    String href
) {
    public static SiteHistoryLinkResponse from(SiteHistoryLink link) {
        return new SiteHistoryLinkResponse(link.getId(), link.getLabel(), link.getHref());
    }
}
