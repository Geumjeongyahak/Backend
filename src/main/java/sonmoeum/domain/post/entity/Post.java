package sonmoeum.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import sonmoeum.domain.base.entity.BaseEntity;
import sonmoeum.domain.channel.entity.Channel;
import sonmoeum.domain.post.enums.PostStatus;
import sonmoeum.domain.post.enums.PostType;
import sonmoeum.domain.users.entity.User;

import java.util.Objects;

@Entity
@Getter
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Setter
    @ManyToOne
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Setter
    @ManyToOne
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
    @Column(name = "post_type", nullable = false, length = 20)
    private PostType postType;

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
    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Setter
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Builder
    public Post(
            @NonNull Channel channel,
            @NonNull User author,
            @NonNull String title,
            @NonNull String contentHtml,
            @NonNull PostType postType,
            PostStatus status,
            Boolean isPinned,
            Boolean allowComment
    ) {
        this.channel = channel;
        this.author = author;
        this.title = title;
        this.contentHtml = contentHtml;
        this.postType = postType;
        this.status = status == null ? PostStatus.PUBLISHED : status;
        this.isPinned = isPinned != null && isPinned;
        this.allowComment = allowComment != null && allowComment;
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
            PostType postType,
            PostStatus status,
            Boolean isPinned,
            Boolean allowComment
    ) {
        if (title != null) {
            this.title = title;
        }
        if (contentHtml != null) {
            this.contentHtml = contentHtml;
        }
        if (postType != null) {
            this.postType = postType;
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
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
