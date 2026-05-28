package geumjeongyahak.domain.sitecontent.v1.dto.response;

import geumjeongyahak.domain.sitecontent.entity.SiteContent;
import geumjeongyahak.domain.sitecontent.entity.SiteContentItem;
import java.util.Comparator;
import java.util.List;

public record SiteContentDepartmentResponse(
    Long id,
    String title,
    String name,
    List<String> responsibilities
) {
    public static SiteContentDepartmentResponse defaultPrincipal() {
        return new SiteContentDepartmentResponse(0L, "교장", "", List.of());
    }

    public static SiteContentDepartmentResponse from(SiteContent content) {
        return new SiteContentDepartmentResponse(
            content.getId(),
            content.getTitle(),
            content.getName(),
            content.getItems().stream()
                .sorted(Comparator
                    .comparingInt(SiteContentItem::getSortOrder)
                    .thenComparing(SiteContentItem::getId))
                .map(SiteContentItem::getContent)
                .toList()
        );
    }
}
