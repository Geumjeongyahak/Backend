package sonmoeum.domain.post.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.common.event.EventPublisher;
import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.ErrorCode;
import sonmoeum.common.exception.ResourceNotFoundException;
import sonmoeum.domain.base.dto.response.PaginationResponse;
import sonmoeum.domain.channel.entity.Channel;
import sonmoeum.domain.channel.service.ChannelProxyService;
import sonmoeum.domain.post.entity.Post;
import sonmoeum.domain.post.event.PostChangedEvent;
import sonmoeum.domain.post.enums.PostStatus;
import sonmoeum.domain.post.enums.PostType;
import sonmoeum.domain.post.repository.PostRepository;
import sonmoeum.domain.post.repository.PostSpecs;
import sonmoeum.domain.post.v1.dto.request.CreatePostRequest;
import sonmoeum.domain.post.v1.dto.request.PostSearchRequest;
import sonmoeum.domain.post.v1.dto.request.UpdatePostRequest;
import sonmoeum.domain.post.v1.dto.response.PostDetailResponse;
import sonmoeum.domain.post.v1.dto.response.PostSummaryResponse;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.exception.UserNotFoundException;
import sonmoeum.domain.users.service.UserProxyService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCrudService {
    private final PostRepository postRepository;
    private final ChannelProxyService channelProxyService;
    private final UserProxyService userProxyService;
    private final PostPermissionService postPermissionService;
    private final PostSearchSpecificationBuilder postSearchSpecificationBuilder;
    private final EventPublisher eventPublisher;

    @Transactional
    public PostDetailResponse createPost(Long channelId, Long userId, boolean isAdminOrManager, CreatePostRequest request) {
        User author = userProxyService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Channel channel = channelProxyService.getReadableById(channelId);

        postPermissionService.validateCreatePermission(userId, isAdminOrManager, channel);

        Post savedPost = postRepository.save(Post.builder()
                .channel(channel)
                .author(author)
                .title(request.title())
                .contentHtml(request.contentHtml())
                .postType(parsePostType(request.postType()))
                .status(resolveStatus(request.status()))
                .isPinned(request.isPinned())
                .allowComment(request.allowComment())
                .build());

        publishPostChangedEvent(channel.getId());
        return PostDetailResponse.from(savedPost);
    }

    public PaginationResponse<PostSummaryResponse> getPosts(Long channelId, PostSearchRequest request) {
        channelProxyService.getReadableById(channelId);

        Specification<Post> spec = postSearchSpecificationBuilder.build(request)
                .and(PostSpecs.hasChannelId(channelId));

        return PaginationResponse.from(
                postRepository.findAll(spec, request.toRequest()),
                PostSummaryResponse::from
        );
    }

    public PaginationResponse<PostSummaryResponse> getPosts(PostSearchRequest request) {
        return PaginationResponse.from(
                postRepository.findAll(postSearchSpecificationBuilder.build(request), request.toRequest()),
                PostSummaryResponse::from
        );
    }

    @Transactional
    public PostDetailResponse getPost(Long channelId, Long postId) {
        channelProxyService.getReadableById(channelId);
        Post post = getPostWithoutDeleted(channelId, postId);
        post.incrementViewCount();
        return PostDetailResponse.from(post);
    }

    @Transactional
    public PostDetailResponse updatePost(
            Long channelId,
            Long userId,
            boolean isAdminOrManager,
            Long postId,
            UpdatePostRequest request
    ) {
        channelProxyService.getReadableById(channelId);
        Post post = getPostWithoutDeleted(channelId, postId);
        postPermissionService.validateEditPermission(userId, isAdminOrManager, post);

        if (!hasAnyUpdate(request)) {
            throw new BusinessException(ErrorCode.NO_CHANGES_DETECTED);
        }

        post.update(
                request.title(),
                request.contentHtml(),
                parsePostType(request.postType()),
                parsePostStatus(request.status()),
                request.isPinned(),
                request.allowComment()
        );

        Post updated = postRepository.save(post);
        publishPostChangedEvent(channelId);
        return PostDetailResponse.from(updated);
    }

    @Transactional
    public void deletePost(Long channelId, Long userId, boolean isAdminOrManager, Long postId) {
        channelProxyService.getReadableById(channelId);
        Post post = getPostWithoutDeleted(channelId, postId);
        postPermissionService.validateEditPermission(userId, isAdminOrManager, post);
        post.delete();
        postRepository.save(post);
        publishPostChangedEvent(channelId);
    }

    private Post getPostWithoutDeleted(Long channelId, Long postId) {
        Post post = postRepository.findByIdAndChannelId(postId, channelId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));
        if (post.isDeleted() || !post.belongsTo(channelId)) {
            throw new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    private boolean hasAnyUpdate(UpdatePostRequest request) {
        return request.title() != null
                || request.contentHtml() != null
                || request.postType() != null
                || request.status() != null
                || request.isPinned() != null
                || request.allowComment() != null;
    }

    private void publishPostChangedEvent(Long channelId) {
        LocalDateTime lastPostedAt = postRepository.findFirstByChannelIdAndIsDeletedFalseOrderByCreatedAtDescIdDesc(channelId)
                .map(Post::getCreatedAt)
                .orElse(null);
        eventPublisher.publish(new PostChangedEvent(channelId, lastPostedAt));
    }

    private PostStatus resolveStatus(String status) {
        return status == null ? PostStatus.PUBLISHED : parsePostStatus(status);
    }

    private PostStatus parsePostStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return PostStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 게시글 상태입니다.");
        }
    }

    private PostType parsePostType(String postType) {
        if (postType == null) {
            return null;
        }
        try {
            return PostType.valueOf(postType);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 게시글 유형입니다.");
        }
    }
}
