package geumjeongyahak.domain.sitecontent.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.file.entity.File;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "site_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteHistory extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "history_date", nullable = false)
    private LocalDate historyDate;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "history", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<SiteHistoryLink> links = new ArrayList<>();

    @OneToMany(mappedBy = "history", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<SiteHistoryPhoto> photos = new ArrayList<>();

    @Builder
    public SiteHistory(
        @NonNull String title,
        String detail,
        @NonNull LocalDate historyDate,
        Integer sortOrder
    ) {
        this.title = title;
        this.detail = detail;
        this.historyDate = historyDate;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }

    public void update(String title, String detail, LocalDate historyDate, Integer sortOrder) {
        this.title = title;
        this.detail = detail;
        this.historyDate = historyDate;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    public void replaceLinks(List<LinkValue> linkValues) {
        this.links.clear();
        if (linkValues == null) {
            return;
        }
        for (int i = 0; i < linkValues.size(); i++) {
            LinkValue value = linkValues.get(i);
            this.links.add(new SiteHistoryLink(this, value.label(), value.href(), i + 1));
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

    public record LinkValue(String label, String href) {
    }
}
