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
import geumjeongyahak.domain.channel.service.ChannelAccessService;
import geumjeongyahak.domain.channel.service.ChannelProxyService;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.event.PostChangedEvent;
import geumjeongyahak.domain.post.exception.PostErrorCode;
import geumjeongyahak.domain.post.enums.PostStatus;
import geumjeongyahak.domain.post.enums.PostType;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCrudService {
    private final PostRepository postRepository;
    private final ChannelProxyService channelProxyService;
    private final ChannelAccessService channelAccessService;
    private final UserProxyService userProxyService;
    private final PostSearchSpecificationBuilder postSearchSpecificationBuilder;
    private final EventPublisher eventPublisher;

    @Transactional
    public PostDetailResponse createPost(Long channelId, CustomUserDetails userDetails, CreatePostRequest request) {
        User author = userProxyService.findById(userDetails.getUserId())
                .orElseThrow(() -> new UserNotFoundException(userDetails.getUserId()));
        Channel channel = channelProxyService.getActiveById(channelId);

        Post savedPost = postRepository.save(Post.builder()
                .channel(channel)
                .author(author)
                .title(request.title())
                .contentHtml(request.contentHtml())
                .postType(PostType.valueOf(request.postType()))
                .status(request.status() == null ? PostStatus.PUBLISHED : PostStatus.valueOf(request.status()))
                .isPinned(request.isPinned())
                .allowComment(request.allowComment())
                .build());

        publishPostChangedEvent(channel.getId());
        return PostDetailResponse.from(savedPost);
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

    @Transactional
    public PostDetailResponse getPost(Long channelId, Long postId) {
        Post post = getPostWithoutDeleted(channelId, postId);
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
        channelAccessService.validateCanManagePost(userDetails, post);

        if (!hasAnyUpdate(request)) {
            throw new BusinessException(CommonErrorCode.NO_CHANGES_DETECTED);
        }

        post.update(
                request.title(),
                request.contentHtml(),
                request.postType() == null ? null : PostType.valueOf(request.postType()),
                request.status() == null ? null : PostStatus.valueOf(request.status()),
                request.isPinned(),
                request.allowComment()
        );

        Post updated = postRepository.save(post);
        publishPostChangedEvent(channelId);
        return PostDetailResponse.from(updated);
    }

    @Transactional
    public void deletePost(Long channelId, CustomUserDetails userDetails, Long postId) {
        Post post = getPostWithoutDeleted(channelId, postId);
        channelAccessService.validateCanManagePost(userDetails, post);
        post.delete();
        postRepository.save(post);
        publishPostChangedEvent(channelId);
    }

    private Post getPostWithoutDeleted(Long channelId, Long postId) {
        Post post = postRepository.findByIdAndChannelId(postId, channelId)
                .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));
        if (post.isDeleted() || !post.belongsTo(channelId)) {
            throw new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND);
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
}
