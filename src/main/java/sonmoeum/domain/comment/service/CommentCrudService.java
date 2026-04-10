package sonmoeum.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.ErrorCode;
import sonmoeum.common.exception.ResourceNotFoundException;
import sonmoeum.domain.channel.service.ChannelProxyService;
import sonmoeum.domain.comment.entity.Comment;
import sonmoeum.domain.comment.repository.CommentRepository;
import sonmoeum.domain.comment.v1.dto.request.CreateCommentRequest;
import sonmoeum.domain.comment.v1.dto.response.CommentResponse;
import sonmoeum.domain.post.entity.Post;
import sonmoeum.domain.post.service.PostProxyService;
import sonmoeum.domain.users.entity.User;
import sonmoeum.domain.users.exception.UserNotFoundException;
import sonmoeum.domain.users.service.UserProxyService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentCrudService {

    private final CommentRepository commentRepository;
    private final ChannelProxyService channelProxyService;
    private final PostProxyService postProxyService;
    private final UserProxyService userProxyService;

    @Transactional
    public CommentResponse createComment(Long channelId, Long postId, Long userId, CreateCommentRequest request) {
        channelProxyService.getReadableById(channelId);
        Post post = postProxyService.getActiveByChannelId(channelId, postId);
        User author = userProxyService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!post.isAllowComment()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "댓글이 허용되지 않은 게시글입니다.");
        }

        Comment parentComment = resolveParentComment(postId, request.parentCommentId());

        Comment savedComment = commentRepository.save(Comment.builder()
                .post(post)
                .author(author)
                .parentComment(parentComment)
                .content(request.content())
                .build());

        return CommentResponse.from(savedComment);
    }

    public List<CommentResponse> getComments(Long channelId, Long postId) {
        channelProxyService.getReadableById(channelId);
        postProxyService.getActiveByChannelId(channelId, postId);

        return commentRepository.findAllByPostIdAndIsDeletedFalseOrderByCreatedAtAscIdAsc(postId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public void deleteComment(Long channelId, Long postId, Long commentId, Long userId, boolean isAdminOrManager) {
        channelProxyService.getReadableById(channelId);
        postProxyService.getActiveByChannelId(channelId, postId);
        Comment comment = getActiveComment(postId, commentId);

        if (!isAdminOrManager && !comment.getAuthor().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        comment.delete();
        commentRepository.save(comment);
    }

    private Comment resolveParentComment(Long postId, Long parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }

        Comment parentComment = getActiveComment(postId, parentCommentId);
        if (parentComment.getParentComment() != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "대댓글에는 다시 답글을 달 수 없습니다.");
        }
        return parentComment;
    }

    private Comment getActiveComment(Long postId, Long commentId) {
        Comment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            throw new ResourceNotFoundException(ErrorCode.COMMENT_NOT_FOUND);
        }

        return comment;
    }
}
