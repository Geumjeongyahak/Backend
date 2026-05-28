package geumjeongyahak.domain.sitecontent.v1.dto.response;

import geumjeongyahak.domain.sitecontent.entity.SiteContent;
import geumjeongyahak.domain.sitecontent.entity.SiteContentItem;
import java.util.Comparator;
import java.util.List;

public record SiteContentClassResponse(
    Long id,
    String name,
    List<String> description
) {
    public static SiteContentClassResponse from(SiteContent content) {
        return new SiteContentClassResponse(
            content.getId(),
            content.getTitle(),
            content.getItems().stream()
                .sorted(Comparator
                    .comparingInt(SiteContentItem::getSortOrder)
                    .thenComparing(SiteContentItem::getId))
                .map(SiteContentItem::getContent)
                .toList()
        );
    }
}
