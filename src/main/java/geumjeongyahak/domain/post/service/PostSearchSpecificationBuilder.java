package geumjeongyahak.domain.post.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.enums.PostStatus;
import geumjeongyahak.domain.post.repository.PostSpecs;
import geumjeongyahak.domain.post.v1.dto.request.PostBoardSearchRequest;
import geumjeongyahak.domain.post.v1.dto.request.PostSearchRequest;

/**
 * 게시글 검색 조건을 Specification 으로 변환한다.
 */
@Component
public class PostSearchSpecificationBuilder {

    public Specification<Post> build(PostSearchRequest request) {
        return applyCommonFilters(baseSpec(), request);
    }

    public Specification<Post> build(PostBoardSearchRequest request) {
        Specification<Post> spec = applyCommonFilters(baseSpec(), request);
        if (request.getChannelId() != null) {
            spec = spec.and(PostSpecs.hasChannelId(request.getChannelId()));
        }
        if (request.getChannelType() != null && !request.getChannelType().isBlank()
                && !"ALL".equalsIgnoreCase(request.getChannelType())) {
            spec = spec.and(PostSpecs.hasChannelType(ChannelType.valueOf(request.getChannelType())));
        }
        if (request.getClassroomId() != null) {
            spec = spec.and(PostSpecs.hasChannelType(ChannelType.CLASSROOM))
                    .and(PostSpecs.hasChannelRefId(request.getClassroomId()));
        }
        if (request.getDepartmentId() != null) {
            spec = spec.and(PostSpecs.hasChannelType(ChannelType.DEPARTMENT))
                    .and(PostSpecs.hasChannelRefId(request.getDepartmentId()));
        }
        return spec;
    }

    private Specification<Post> baseSpec() {
        return PostSpecs.withoutDeleted()
                .and((root, query, cb) -> cb.isFalse(root.get("channel").get("isDeleted")));
    }

    private Specification<Post> applyCommonFilters(Specification<Post> spec, PostSearchRequest request) {
        if (request.getAuthor() != null && !request.getAuthor().isBlank()) {
            spec = spec.and(PostSpecs.containsAuthor(request.getAuthor()));
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            spec = spec.and(PostSpecs.containsTitle(request.getTitle()));
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            spec = spec.and(PostSpecs.containsContent(request.getContent()));
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            spec = spec.and(PostSpecs.hasStatus(PostStatus.valueOf(request.getStatus())));
        }
        if (request.getIsPinned() != null) {
            spec = spec.and(PostSpecs.hasIsPinned(request.getIsPinned()));
        }
        return spec;
    }
}
