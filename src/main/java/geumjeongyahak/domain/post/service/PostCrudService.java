package geumjeongyahak.domain.post.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.service.ChannelProxyService;
import geumjeongyahak.domain.post.config.PostProperties;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.event.PostChangedEvent;
import geumjeongyahak.domain.post.exception.PostErrorCode;
import geumjeongyahak.domain.post.enums.PostStatus;
import geumjeongyahak.domain.post.repository.PostRepository;
import geumjeongyahak.domain.post.repository.PostSpecs;
import geumjeongyahak.domain.post.v1.dto.request.CreatePostRequest;
import geumjeongyahak.domain.post.v1.dto.request.PostSearchRequest;
import geumjeongyahak.domain.post.v1.dto.request.UpdatePostRequest;
import geumjeongyahak.domain.post.v1.dto.response.PostDetailResponse;
import geumjeongyahak.domain.post.v1.dto.response.PostSummaryResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.UserNotFoundException;
import geumjeongyahak.domain.users.service.UserProxyService;
import geumjeongyahak.common.security.service.CustomUserDetails;

import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCrudService {
    private final PostRepository postRepository;
    private final ChannelProxyService channelProxyService;
    private final UserProxyService userProxyService;
    private final PostSearchSpecificationBuilder postSearchSpecificationBuilder;
    private final EventPublisher eventPublisher;
    private final PostProperties postProperties;

    @Transactional
    public PostDetailResponse createPost(Long channelId, CustomUserDetails userDetails, CreatePostRequest request) {
        User author = userProxyService.findById(userDetails.getUserId())
                .orElseThrow(() -> new UserNotFoundException(userDetails.getUserId()));
        Channel channel = channelProxyService.getActiveById(channelId);
        PostStatus status = request.status() == null ? PostStatus.PUBLISHED : PostStatus.valueOf(request.status());

        Post savedPost = postRepository.save(Post.builder()
                .channel(channel)
                .author(author)
                .title(request.title())
                .contentHtml(request.contentHtml())
                .status(status)
                .isPinned(request.isPinned())
                .allowComment(request.allowComment())
                .thumbnailUrl(request.thumbnailUrl())
                .expiresAt(resolveDraftExpiration(status))
                .build());

        publishPostChangedEvent(channel.getId());
        return reloadForResponse(channelId, savedPost.getId());
    }

    public PaginationResponse<PostSummaryResponse> getPosts(Long channelId, CustomUserDetails userDetails, PostSearchRequest request) {
        Specification<Post> spec = postSearchSpecificationBuilder.build(request, userDetails)
                .and(PostSpecs.hasChannelId(channelId));

        return PaginationResponse.from(
                postRepository.findAll(spec, request.toRequest()),
                PostSummaryResponse::from
        );
    }

    public PaginationResponse<PostSummaryResponse> getPosts(CustomUserDetails userDetails, PostSearchRequest request) {
        return PaginationResponse.from(
                postRepository.findAll(postSearchSpecificationBuilder.build(request, userDetails), request.toRequest()),
                PostSummaryResponse::from
        );
    }

    public PaginationResponse<PostSummaryResponse> getMyDrafts(Long channelId, CustomUserDetails userDetails, Pageable pageable) {
        return PaginationResponse.from(
                postRepository.findAllByChannelIdAndAuthorIdAndStatusAndIsDeletedFalse(
                        channelId, userDetails.getUserId(), PostStatus.DRAFT, pageable),
                PostSummaryResponse::from
        );
    }

    @Transactional
    public PostDetailResponse getPost(Long channelId, Long postId) {
        Post post = postRepository.findWithAttachmentsByIdAndChannelId(postId, channelId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));
        post.incrementViewCount();
        return PostDetailResponse.from(post);
    }

    @Transactional
    public PostDetailResponse updatePost(
            Long channelId,
            CustomUserDetails userDetails,
            Long postId,
            UpdatePostRequest request
    ) {
        Post post = getPostWithoutDeleted(channelId, postId);

        if (!hasAnyUpdate(request)) {
            throw new BusinessException(CommonErrorCode.NO_CHANGES_DETECTED);
        }

        PostStatus targetStatus = request.status() == null
                ? post.getStatus()
                : PostStatus.valueOf(request.status());

        post.update(
                request.title(),
                request.contentHtml(),
                request.status() == null ? null : targetStatus,
                null,
                request.allowComment(),
                request.thumbnailUrl(),
                null
        );

        if (targetStatus == PostStatus.PUBLISHED) {
            post.publish();
        } else if (targetStatus == PostStatus.ARCHIVED) {
            post.archive();
        } else if (targetStatus == PostStatus.DRAFT) {
            post.updateDraftExpiration(resolveDraftExpiration(targetStatus));
        }

        postRepository.save(post);
        publishPostChangedEvent(channelId);
        return reloadForResponse(channelId, postId);
    }

    @Transactional
    public void deletePost(Long channelId, CustomUserDetails userDetails, Long postId) {
        Post post = getPostWithoutDeleted(channelId, postId);
        post.delete();
        postRepository.save(post);
        publishPostChangedEvent(channelId);
    }

    private PostDetailResponse reloadForResponse(Long channelId, Long postId) {
        return postRepository.findWithAttachmentsByIdAndChannelId(postId, channelId)
                .filter(p -> !p.isDeleted())
                .map(PostDetailResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));
    }

    private Post getPostWithoutDeleted(Long channelId, Long postId) {
        Post post = postRepository.findByIdAndChannelId(postId, channelId)
                .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));
        if (post.isDeleted() || !post.belongsTo(channelId)) {
            throw new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    @Transactional
    public PostDetailResponse pinPost(Long channelId, Long postId, boolean isPinned) {
        Post post = getPostWithoutDeleted(channelId, postId);
        post.update(null, null, null, isPinned, null, null, null);
        postRepository.save(post);
        return reloadForResponse(channelId, postId);
    }

    private boolean hasAnyUpdate(UpdatePostRequest request) {
        return request.title() != null
                || request.contentHtml() != null
                || request.status() != null
                || request.allowComment() != null
                || request.thumbnailUrl() != null;
    }

    private LocalDateTime resolveDraftExpiration(PostStatus status) {
        if (status != PostStatus.DRAFT) {
            return null;
        }
        return LocalDateTime.now().plusMinutes(postProperties.getDraftExpirationMinutes());
    }

    private void publishPostChangedEvent(Long channelId) {
        LocalDateTime lastPostedAt = postRepository.findFirstByChannelIdAndStatusAndIsDeletedFalseOrderByCreatedAtDescIdDesc(channelId, PostStatus.PUBLISHED)
                .map(Post::getCreatedAt)
                .orElse(null);
        eventPublisher.publish(new PostChangedEvent(channelId, lastPostedAt));
    }
}
