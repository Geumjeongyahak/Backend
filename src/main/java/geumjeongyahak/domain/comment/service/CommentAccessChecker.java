package geumjeongyahak.domain.comment.service;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("commentAccess")
@RequiredArgsConstructor
public class CommentAccessChecker {
    private final CommentRepository commentRepository;

    public boolean can(Long commentId, CustomUserDetails userDetail) {
        if (commentId == null || userDetail == null) return false;

        return commentRepository.findAuthorIdById(commentId)
                .map(authorId -> authorId.equals(userDetail.getUserId()))
                .orElse(false);
    }
}
