package sonmoeum.domain.post.repository;

import org.springframework.data.jpa.domain.Specification;
import sonmoeum.domain.channel.enums.ChannelType;
import sonmoeum.domain.post.entity.Post;
import sonmoeum.domain.post.enums.PostStatus;
import sonmoeum.domain.post.enums.PostType;

public final class PostSpecs {

    private PostSpecs() {
    }

    public static Specification<Post> withoutDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<Post> hasVisibleChannel() {
        return (root, query, cb) -> cb.and(
                cb.isFalse(root.get("channel").get("isDeleted")),
                cb.isTrue(root.get("channel").get("isActive"))
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

    public static Specification<Post> hasPostType(PostType postType) {
        return (root, query, cb) -> cb.equal(root.get("postType"), postType);
    }

    public static Specification<Post> hasStatus(PostStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Post> hasIsPinned(Boolean isPinned) {
        return (root, query, cb) -> cb.equal(root.get("isPinned"), isPinned);
    }
}
