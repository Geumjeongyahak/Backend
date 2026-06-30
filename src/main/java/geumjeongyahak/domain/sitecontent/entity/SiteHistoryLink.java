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
@Table(name = "site_history_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteHistoryLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id", nullable = false)
    private SiteHistory history;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String href;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public SiteHistoryLink(@NonNull SiteHistory history, @NonNull String label, @NonNull String href, int sortOrder) {
        this.history = history;
        this.label = label;
        this.href = href;
        this.sortOrder = sortOrder;
    }
}
