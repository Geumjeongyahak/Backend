package geumjeongyahak.domain.sitecontent.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.file.entity.File;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "site_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteHistory extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "link_label", length = 120)
    private String linkLabel;

    @Column(name = "link_href", columnDefinition = "TEXT")
    private String linkHref;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "history", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SiteHistoryPhoto> photos = new ArrayList<>();

    @Builder
    public SiteHistory(
        @NonNull String title,
        String detail,
        String linkLabel,
        String linkHref,
        Integer sortOrder
    ) {
        this.title = title;
        this.detail = detail;
        this.linkLabel = linkLabel;
        this.linkHref = linkHref;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }

    public void update(String title, String detail, String linkLabel, String linkHref, Integer sortOrder) {
        this.title = title;
        this.detail = detail;
        this.linkLabel = linkLabel;
        this.linkHref = linkHref;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    public void replacePhotos(List<PhotoValue> photoValues) {
        this.photos.clear();
        if (photoValues == null) {
            return;
        }
        for (int i = 0; i < photoValues.size(); i++) {
            PhotoValue value = photoValues.get(i);
            this.photos.add(new SiteHistoryPhoto(this, value.file(), value.src(), value.alt(), i + 1));
        }
    }

    public record PhotoValue(File file, String src, String alt) {
    }
}
