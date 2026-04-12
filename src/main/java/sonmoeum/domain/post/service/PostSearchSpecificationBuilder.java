package sonmoeum.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.CommonErrorCode;
import sonmoeum.domain.channel.enums.ChannelType;
import sonmoeum.domain.channel.service.ChannelProxyService;
import sonmoeum.domain.post.entity.Post;
import sonmoeum.domain.post.enums.PostStatus;
import sonmoeum.domain.post.enums.PostType;
import sonmoeum.domain.post.repository.PostSpecs;
import sonmoeum.domain.post.v1.dto.request.PostSearchRequest;

/**
 * 게시글 검색 조건을 Specification 으로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class PostSearchSpecificationBuilder {

    private final ChannelProxyService channelProxyService;

    public Specification<Post> build(PostSearchRequest request) {
        Specification<Post> spec = PostSpecs.withoutDeleted()
                .and(PostSpecs.hasVisibleChannel());

        if (request.getChannelId() != null) {
            channelProxyService.getReadableById(request.getChannelId());
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
            spec = spec.and(PostSpecs.hasPostType(parsePostType(request.getPostType())));
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            spec = spec.and(PostSpecs.hasStatus(parsePostStatus(request.getStatus())));
        }
        if (request.getChannelType() != null && !request.getChannelType().isBlank()) {
            spec = spec.and(PostSpecs.hasChannelType(parseChannelType(request.getChannelType())));
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

    private PostStatus parsePostStatus(String status) {
        try {
            return PostStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "유효하지 않은 게시글 상태입니다.");
        }
    }

    private PostType parsePostType(String postType) {
        try {
            return PostType.valueOf(postType);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "유효하지 않은 게시글 유형입니다.");
        }
    }

    private ChannelType parseChannelType(String channelType) {
        try {
            return ChannelType.valueOf(channelType);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "유효하지 않은 채널 유형입니다.");
        }
    }
}
