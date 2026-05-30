package geumjeongyahak.domain.sitecontent.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.file.entity.File;
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
@Table(name = "site_history_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteHistoryPhoto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id", nullable = false)
    private SiteHistory history;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String src;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    @Column(length = 255)
    private String alt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public SiteHistoryPhoto(@NonNull SiteHistory history, File file, @NonNull String src, String alt, int sortOrder) {
        this.history = history;
        this.file = file;
        this.src = src;
        this.alt = alt;
        this.sortOrder = sortOrder;
    }
}
