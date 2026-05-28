package geumjeongyahak.domain.sitecontent.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.sitecontent.enums.SiteContentGroup;
import geumjeongyahak.domain.sitecontent.enums.SiteContentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "site_contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteContent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private SiteContentType contentType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_group", length = 30)
    private SiteContentGroup group;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "siteContent", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SiteContentItem> items = new ArrayList<>();

    @Builder
    public SiteContent(
        @NonNull SiteContentType contentType,
        Long refId,
        @NonNull String title,
        String name,
        SiteContentGroup group,
        Integer sortOrder
    ) {
        this.contentType = contentType;
        this.refId = refId;
        this.title = title;
        this.name = name;
        this.group = group;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }

    public void update(
        SiteContentType contentType,
        Long refId,
        String title,
        String name,
        SiteContentGroup group,
        Integer sortOrder
    ) {
        this.contentType = contentType;
        this.refId = refId;
        this.title = title;
        this.name = name;
        this.group = group;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    public void replaceItems(List<String> contents) {
        this.items.clear();
        if (contents == null) {
            return;
        }
        for (int i = 0; i < contents.size(); i++) {
            this.items.add(new SiteContentItem(this, contents.get(i), i + 1));
        }
    }
}
