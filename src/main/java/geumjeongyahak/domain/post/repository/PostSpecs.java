package geumjeongyahak.domain.post.repository;

import org.springframework.data.jpa.domain.Specification;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.enums.PostStatus;

public final class PostSpecs {

    private PostSpecs() {
    }

    public static Specification<Post> withoutDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<Post> hasPublicAccess() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("channel").get("isActive")),
                cb.notEqual(root.get("channel").get("accessLevel"), ChannelAccessLevel.CLOSED)
        );
    }

    public static Specification<Post> hasAnyChannelId(java.util.Collection<Long> channelIds) {
        return (root, query, cb) -> root.get("channel").get("id").in(channelIds);
    }

    public static Specification<Post> hasVisibleChannel() {
        return (root, query, cb) -> cb.and(
                cb.isFalse(root.get("channel").get("isDeleted")),
                cb.isTrue(root.get("channel").get("isActive")),
                cb.notEqual(root.get("channel").get("accessLevel"), ChannelAccessLevel.CLOSED)
        );
    }

    public static Specification<Post> hasChannelId(Long channelId) {
        return (root, query, cb) -> cb.equal(root.get("channel").get("id"), channelId);
    }

    public static Specification<Post> hasChannelType(ChannelType channelType) {
        return (root, query, cb) -> cb.equal(root.get("channel").get("channelType"), channelType);
    }

    public static Specification<Post> hasChannelRefId(Long refId) {
        return (root, query, cb) -> cb.equal(root.get("channel").get("refId"), refId);
    }

    public static Specification<Post> containsAuthor(String authorKeyword) {
        return (root, query, cb) -> cb.or(
                cb.like(root.get("author").get("name"), "%" + authorKeyword + "%"),
                cb.like(root.get("author").get("username"), "%" + authorKeyword + "%")
        );
    }

    public static Specification<Post> containsTitle(String titleKeyword) {
        return (root, query, cb) -> cb.like(root.get("title"), "%" + titleKeyword + "%");
    }

    public static Specification<Post> containsContent(String contentKeyword) {
        return (root, query, cb) -> cb.like(root.get("contentHtml"), "%" + contentKeyword + "%");
    }

    public static Specification<Post> hasStatus(PostStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Post> hasIsPinned(Boolean isPinned) {
        return (root, query, cb) -> cb.equal(root.get("isPinned"), isPinned);
    }
}
