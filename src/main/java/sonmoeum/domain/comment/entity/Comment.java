package sonmoeum.domain.comment.entity;

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
import sonmoeum.domain.base.entity.BaseEntity;
import sonmoeum.domain.comment.enums.CommentStatus;
import sonmoeum.domain.post.entity.Post;
import sonmoeum.domain.users.entity.User;

@Entity
@Getter
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Builder
    public Comment(
            @NonNull Post post,
            @NonNull User author,
            Comment parentComment,
            @NonNull String content
    ) {
        this.post = post;
        this.author = author;
        this.parentComment = parentComment;
        this.content = content;
        this.status = CommentStatus.ACTIVE;
        this.isDeleted = false;
    }

    public void delete() {
        this.isDeleted = true;
    }
}
