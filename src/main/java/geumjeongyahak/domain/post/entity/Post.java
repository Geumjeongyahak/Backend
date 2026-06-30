package geumjeongyahak.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import geumjeongyahak.domain.base.entity.BaseEntity;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.post.enums.PostStatus;
import geumjeongyahak.domain.users.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Entity
@Getter
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Setter
    @Column(nullable = false, length = 255)
    private String title;

    @Setter
    @Column(name = "content_html", nullable = false, columnDefinition = "TEXT")
    private String contentHtml;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Setter
    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned;

    @Setter
    @Column(name = "allow_comment", nullable = false)
    private boolean allowComment;

    @Setter
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Setter
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Setter
    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Setter
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 20)
    private List<PostAttachment> postAttachments = new ArrayList<>();

    @Builder
    public Post(
            @NonNull Channel channel,
            @NonNull User author,
            @NonNull String title,
            @NonNull String contentHtml,
            PostStatus status,
            Boolean isPinned,
            Boolean allowComment,
            String thumbnailUrl,
            LocalDateTime expiresAt
    ) {
        this.channel = channel;
        this.author = author;
        this.title = title;
        this.contentHtml = contentHtml;
        this.status = status == null ? PostStatus.PUBLISHED : status;
        this.isPinned = isPinned != null && isPinned;
        this.allowComment = allowComment != null && allowComment;
        this.thumbnailUrl = thumbnailUrl;
        this.expiresAt = expiresAt;
        this.viewCount = 0L;
        this.isDeleted = false;
    }

    public boolean belongsTo(Long channelId) {
        return channelId != null
                && this.channel != null
                && Objects.equals(this.channel.getId(), channelId);
    }

    public void update(
            String title,
            String contentHtml,
            PostStatus status,
            Boolean isPinned,
            Boolean allowComment,
            String thumbnailUrl,
            LocalDateTime expiresAt
    ) {
        if (title != null) {
            this.title = title;
        }
        if (contentHtml != null) {
            this.contentHtml = contentHtml;
        }
        if (status != null) {
            this.status = status;
        }
        if (isPinned != null) {
            this.isPinned = isPinned;
        }
        if (allowComment != null) {
            this.allowComment = allowComment;
        }
        if (thumbnailUrl != null) {
            this.thumbnailUrl = thumbnailUrl;
        }
        if (expiresAt != null) {
            this.expiresAt = expiresAt;
        }
    }

    public void updateDraftExpiration(LocalDateTime expiresAt) {
        this.status = PostStatus.DRAFT;
        this.expiresAt = expiresAt;
    }

    public void publish() {
        this.status = PostStatus.PUBLISHED;
        this.expiresAt = null;
    }

    public void archive() {
        this.status = PostStatus.ARCHIVED;
        this.expiresAt = null;
    }

    public void clearThumbnail() {
        this.thumbnailUrl = null;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
