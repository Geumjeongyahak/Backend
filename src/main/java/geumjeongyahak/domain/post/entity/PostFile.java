package geumjeongyahak.domain.post.entity;

import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.file.entity.File;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@Table(name = "post_files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    public PostFile(
            @NonNull Post post,
            @NonNull File file,
            Integer sortOrder
    ) {
        this.post = post;
        this.file = file;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }
}
