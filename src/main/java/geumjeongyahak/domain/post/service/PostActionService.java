package geumjeongyahak.domain.post.service;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.service.ChannelProxyService;
import geumjeongyahak.domain.post.config.PostProperties;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.enums.PostStatus;
import geumjeongyahak.domain.post.event.PostChangedEvent;
import geumjeongyahak.domain.post.event.PostPublishedEvent;
import geumjeongyahak.domain.post.exception.PostErrorCode;
import geumjeongyahak.domain.post.repository.PostFileRepository;
import geumjeongyahak.domain.post.repository.PostRepository;
import geumjeongyahak.domain.post.v1.dto.response.PostDetailResponse;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.UserNotFoundException;
import geumjeongyahak.domain.users.service.UserProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostActionService {

    private final PostRepository postRepository;
    private final PostFileRepository postFileRepository;
    private final ChannelProxyService channelProxyService;
    private final UserProxyService userProxyService;
    private final EventPublisher eventPublisher;
    private final PostProperties postProperties;

    @Transactional
    public PostDetailResponse createDraft(Long channelId, CustomUserDetails userDetails) {
        User author = userProxyService.findById(userDetails.getUserId())
                .orElseThrow(() -> new UserNotFoundException(userDetails.getUserId()));
        Channel channel = channelProxyService.getActiveById(channelId);

        Post draft = postRepository.save(Post.builder()
                .channel(channel)
                .author(author)
                .title("")
                .contentHtml("")
                .status(PostStatus.DRAFT)
                .isPinned(false)
                .allowComment(true)
                .thumbnailUrl(null)
                .expiresAt(nextDraftExpiration())
                .build());

        log.info("게시글 초안 생성 완료 - channelId: {}, postId: {}", channelId, draft.getId());
        return reloadForResponse(channelId, draft.getId());
    }

    @Transactional
    public PostDetailResponse saveDraft(
            Long channelId,
            Long postId,
            CustomUserDetails userDetails,
            SaveDraftCommand command
    ) {
        Post draft = getOwnedPost(channelId, postId, userDetails);
        ensureDraft(draft);

        draft.update(
                command.title(),
                command.contentHtml(),
                PostStatus.DRAFT,
                null,
                command.allowComment(),
                command.thumbnailUrl(),
                null
        );
        draft.updateDraftExpiration(nextDraftExpiration());

        postRepository.save(draft);
        log.info("게시글 초안 저장 완료 - channelId: {}, postId: {}", channelId, postId);
        return reloadForResponse(channelId, postId);
    }

    @Transactional
    public PostDetailResponse publish(
            Long channelId,
            Long postId,
            CustomUserDetails userDetails,
            PublishPostCommand command
    ) {
        Post draft = getOwnedPost(channelId, postId, userDetails);
        ensureDraft(draft);
        validatePublishCommand(command);

        String thumbnail = command.thumbnailUrl() != null
                ? command.thumbnailUrl()
                : postFileRepository.findFirstByPostIdOrderBySortOrderAsc(postId)
                        .map(pf -> pf.getFile().getPublicUrl())
                        .orElse(null);

        draft.update(
                command.title(),
                command.contentHtml(),
                PostStatus.PUBLISHED,
                null,
                command.allowComment(),
                thumbnail,
                null
        );
        draft.publish();

        Post publishedPost = postRepository.save(draft);
        publishPostEvents(publishedPost);

        log.info("게시글 발행 완료 - channelId: {}, postId: {}", channelId, postId);
        return reloadForResponse(channelId, postId);
    }

    private PostDetailResponse reloadForResponse(Long channelId, Long postId) {
        return postRepository.findWithAttachmentsByIdAndChannelId(postId, channelId)
                .filter(p -> !p.isDeleted())
                .map(PostDetailResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));
    }

    private Post getOwnedPost(Long channelId, Long postId, CustomUserDetails userDetails) {
        Post post = postRepository.findByIdAndChannelId(postId, channelId)
                .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));

        if (post.isDeleted() || !post.belongsTo(channelId)) {
            throw new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND);
        }
        if (!post.getAuthor().getId().equals(userDetails.getUserId())) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "본인이 작성한 게시글 초안만 편집할 수 있습니다.");
        }
        return post;
    }

    private void ensureDraft(Post post) {
        if (post.getStatus() != PostStatus.DRAFT) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "초안 상태 게시글만 이 작업을 수행할 수 있습니다.");
        }
    }

    private void validatePublishCommand(PublishPostCommand command) {
        if (!StringUtils.hasText(command.title())) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "발행 시 제목은 필수입니다.");
        }
        if (!StringUtils.hasText(command.contentHtml())) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "발행 시 본문은 필수입니다.");
        }
    }

    private LocalDateTime nextDraftExpiration() {
        return LocalDateTime.now().plusMinutes(postProperties.getDraftExpirationMinutes());
    }

    private void publishPostEvents(Post post) {
        LocalDateTime lastPostedAt = postRepository
                .findFirstByChannelIdAndStatusAndIsDeletedFalseOrderByCreatedAtDescIdDesc(post.getChannel().getId(), PostStatus.PUBLISHED)
                .map(Post::getCreatedAt)
                .orElse(post.getCreatedAt());

        eventPublisher.publish(new PostPublishedEvent(post.getId(), post.getChannel().getId(), post.getAuthor().getId()));
        eventPublisher.publish(new PostChangedEvent(post.getChannel().getId(), lastPostedAt));
    }

    public record SaveDraftCommand(
            String title,
            String contentHtml,
            Boolean allowComment,
            String thumbnailUrl
    ) {
    }

    public record PublishPostCommand(
            String title,
            String contentHtml,
            Boolean allowComment,
            String thumbnailUrl
    ) {
    }
}
