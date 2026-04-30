package geumjeongyahak.domain.post.service;

import geumjeongyahak.common.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.channel.service.ChannelProxyService;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.enums.PostStatus;
import geumjeongyahak.domain.post.enums.PostType;
import geumjeongyahak.domain.post.repository.PostSpecs;
import geumjeongyahak.domain.post.v1.dto.request.PostSearchRequest;

/**
 * 게시글 검색 조건을 Specification 으로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class PostSearchSpecificationBuilder {

    private final ChannelProxyService channelProxyService;

    public Specification<Post> build(PostSearchRequest request, CustomUserDetails userDetails) {
        Specification<Post> spec = PostSpecs.withoutDeleted()
                .and(PostSpecs.hasVisibleChannel());

        if (request.getChannelId() != null) {
            channelProxyService.getActiveById(request.getChannelId());
            spec = spec.and(PostSpecs.hasChannelId(request.getChannelId()));
        }
        if (request.getAuthor() != null && !request.getAuthor().isBlank()) {
            spec = spec.and(PostSpecs.containsAuthor(request.getAuthor()));
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            spec = spec.and(PostSpecs.containsTitle(request.getTitle()));
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            spec = spec.and(PostSpecs.containsContent(request.getContent()));
        }
        if (request.getPostType() != null && !request.getPostType().isBlank()) {
            spec = spec.and(PostSpecs.hasPostType(PostType.valueOf(request.getPostType())));
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            spec = spec.and(PostSpecs.hasStatus(PostStatus.valueOf(request.getStatus())));
        }
        if (request.getChannelType() != null && !request.getChannelType().isBlank()) {
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
        if (request.getIsPinned() != null) {
            spec = spec.and(PostSpecs.hasIsPinned(request.getIsPinned()));
        }
        return spec;
    }
}
