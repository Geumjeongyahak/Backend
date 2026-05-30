package geumjeongyahak.domain.post.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.service.ChannelAccessChecker;
import geumjeongyahak.domain.channel.service.ChannelProxyService;
import geumjeongyahak.domain.post.config.PostProperties;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.event.PostChangedEvent;
import geumjeongyahak.domain.post.event.PostDeletedEvent;
import geumjeongyahak.domain.post.exception.PostErrorCode;
import geumjeongyahak.domain.post.enums.PostStatus;
import geumjeongyahak.domain.post.repository.PostRepository;
import geumjeongyahak.domain.post.repository.PostSpecs;
import geumjeongyahak.domain.post.repository.PostAttachmentRepository;
import geumjeongyahak.domain.post.repository.PostFileRepository;
import geumjeongyahak.domain.post.v1.dto.request.PostBoardSearchRequest;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCrudService {
    private final PostRepository postRepository;
    private final ChannelProxyService channelProxyService;
    private final ChannelAccessChecker channelAccessChecker;
    private final UserProxyService userProxyService;
    private final PostSearchSpecificationBuilder postSearchSpecificationBuilder;
    private final EventPublisher eventPublisher;
    private final PostProperties postProperties;
    private final PostFileRepository postFileRepository;
    private final PostAttachmentRepository postAttachmentRepository;
    private final PostContentImageCleanupService postContentImageCleanupService;

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
        ensureContentSearchAllowed(channelId, userDetails, request);

        Specification<Post> spec = postSearchSpecificationBuilder.build(request)
                .and(PostSpecs.hasChannelId(channelId));

        return PaginationResponse.from(
                postRepository.findAll(spec, request.toRequest()),
                PostSummaryResponse::from
        );
    }

    public PaginationResponse<PostSummaryResponse> getPosts(CustomUserDetails userDetails, PostBoardSearchRequest request) {
        ensureContentSearchAllowed(request.getChannelId(), userDetails, request);

        return PaginationResponse.from(
                postRepository.findAll(postSearchSpecificationBuilder.build(request), request.toRequest()),
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
    public PostDetailResponse getPost(Long channelId, Long postId, CustomUserDetails userDetails) {
        if (!channelAccessChecker.can("read", channelId, userDetails)) {
            throw new AccessDeniedException("게시글 상세 조회 권한이 없습니다.");
        }

        Post post = postRepository.findWithAttachmentsByIdAndChannelId(postId, channelId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.PUBLISHED && !canReadUnpublishedPost(post, userDetails)) {
            throw new AccessDeniedException("미발행 게시글 상세 조회 권한이 없습니다.");
        }

        post.incrementViewCount();
        return PostDetailResponse.from(post);
    }

    private boolean canReadUnpublishedPost(Post post, CustomUserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }
        return userDetails.isAdmin() || post.getAuthor().getId().equals(userDetails.getUserId());
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
        postContentImageCleanupService.deleteUnusedImages(postId, post.getContentHtml());
        publishPostChangedEvent(channelId);
        return reloadForResponse(channelId, postId);
    }

    @Transactional
    public void deletePost(Long channelId, CustomUserDetails userDetails, Long postId) {
        Post post = getPostWithoutDeleted(channelId, postId);
        Set<UUID> unreferencedFileIds = findUnreferencedFileIds(postId);

        post.delete();
        postRepository.save(post);
        publishPostChangedEvent(channelId);
        eventPublisher.publish(new PostDeletedEvent(postId, unreferencedFileIds));
    }

    private Set<UUID> findUnreferencedFileIds(Long postId) {
        Set<UUID> fileIds = new HashSet<>();
        fileIds.addAll(postFileRepository.findFileIdsByPostId(postId));
        fileIds.addAll(postAttachmentRepository.findFileIdsByPostId(postId));

        if (fileIds.isEmpty()) {
            return Set.of();
        }

        Set<UUID> referencedFileIds = new HashSet<>();
        referencedFileIds.addAll(postFileRepository.findReferencedFileIdsByFileIdInAndPostIdNot(fileIds, postId));
        referencedFileIds.addAll(postAttachmentRepository.findReferencedFileIdsByFileIdInAndPostIdNot(fileIds, postId));

        fileIds.removeAll(referencedFileIds);
        return fileIds;
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

    private void ensureContentSearchAllowed(Long channelId, CustomUserDetails userDetails, PostSearchRequest request) {
        if (!hasText(request.getContent())) {
            return;
        }

        if (canSearchContent(channelId, userDetails)) {
            return;
        }

        throw new AccessDeniedException("게시글 본문 검색 권한이 없습니다.");
    }

    private boolean canSearchContent(Long channelId, CustomUserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }
        if (userDetails.isAdmin()) {
            return true;
        }
        if (channelId != null && channelAccessChecker.can("read", channelId, userDetails)) {
            return true;
        }
        return hasWildcardChannelPermission(userDetails);
    }

    private boolean hasWildcardChannelPermission(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("channel:read:*")
                        || a.getAuthority().equals("channel:write:*")
                        || a.getAuthority().equals("channel:manage:*"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void publishPostChangedEvent(Long channelId) {
        LocalDateTime lastPostedAt = postRepository.findFirstByChannelIdAndStatusAndIsDeletedFalseOrderByCreatedAtDescIdDesc(channelId, PostStatus.PUBLISHED)
                .map(Post::getCreatedAt)
                .orElse(null);
        eventPublisher.publish(new PostChangedEvent(channelId, lastPostedAt));
    }
}
