package geumjeongyahak.domain.sitecontent.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "site_content_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteContentItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_content_id", nullable = false)
    private SiteContent siteContent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public SiteContentItem(@NonNull SiteContent siteContent, @NonNull String content, int sortOrder) {
        this.siteContent = siteContent;
        this.content = content;
        this.sortOrder = sortOrder;
    }
}
