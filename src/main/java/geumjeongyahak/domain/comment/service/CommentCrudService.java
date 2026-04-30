package geumjeongyahak.domain.comment.service;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.auth.exception.AuthErrorCode;
import geumjeongyahak.domain.comment.entity.Comment;
import geumjeongyahak.domain.comment.exception.CommentErrorCode;
import geumjeongyahak.domain.comment.repository.CommentRepository;
import geumjeongyahak.domain.comment.v1.dto.request.CreateCommentRequest;
import geumjeongyahak.domain.comment.v1.dto.response.CommentResponse;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.service.PostProxyService;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.exception.UserNotFoundException;
import geumjeongyahak.domain.users.service.UserProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentCrudService {

    private final CommentRepository commentRepository;
    private final PostProxyService postProxyService;
    private final UserProxyService userProxyService;

    @Transactional
    public CommentResponse createComment(Long channelId, Long postId, CustomUserDetails userDetails, CreateCommentRequest request) {
        Post post = postProxyService.getActiveByChannelId(channelId, postId);
        User author = userProxyService.findById(userDetails.getUserId())
                .orElseThrow(() -> new UserNotFoundException(userDetails.getUserId()));

        if (!post.isAllowComment()) {
            throw new BusinessException(AuthErrorCode.ACCESS_DENIED);
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
        postProxyService.getActiveByChannelId(channelId, postId);

        return commentRepository.findAllByPostIdAndIsDeletedFalseOrderByCreatedAtAscIdAsc(postId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public void deleteComment(Long channelId, Long postId, Long commentId, CustomUserDetails userDetails) {
        postProxyService.getActiveByChannelId(channelId, postId);
        Comment comment = getActiveComment(postId, commentId);

        comment.delete();
        commentRepository.save(comment);
    }

    private Comment resolveParentComment(Long postId, Long parentCommentId) {
        if (parentCommentId == null) return null;

        Comment parentComment = getActiveComment(postId, parentCommentId);
        if (parentComment.getParentComment() != null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "대댓글에는 다시 답글을 달 수 없습니다.");
        }
        return parentComment;
    }

    private Comment getActiveComment(Long postId, Long commentId) {
        Comment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new ResourceNotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            throw new ResourceNotFoundException(CommentErrorCode.COMMENT_NOT_FOUND);
        }

        return comment;
    }
}
