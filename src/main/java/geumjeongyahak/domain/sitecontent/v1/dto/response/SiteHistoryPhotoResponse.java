package geumjeongyahak.domain.sitecontent.v1.dto.response;

import geumjeongyahak.domain.sitecontent.entity.SiteHistoryPhoto;

public record SiteHistoryPhotoResponse(
    Long id,
    String src,
    String alt
) {
    public static SiteHistoryPhotoResponse from(SiteHistoryPhoto photo) {
        return new SiteHistoryPhotoResponse(photo.getId(), photo.getSrc(), photo.getAlt());
    }
}
