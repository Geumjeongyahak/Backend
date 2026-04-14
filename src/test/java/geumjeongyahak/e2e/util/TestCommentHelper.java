package geumjeongyahak.e2e.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.comment.entity.Comment;
import geumjeongyahak.domain.comment.repository.CommentRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class TestCommentHelper {
    private static final Logger log = LoggerFactory.getLogger(TestCommentHelper.class);

    private final CommentRepository commentRepository;
    private final Map<Long, Comment> commentCache;

    public TestCommentHelper(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
        this.commentCache = new HashMap<>();
    }

    public void registerComment(Long commentId) {
        commentRepository.findById(commentId).ifPresentOrElse(
                comment -> commentCache.put(commentId, comment),
                () -> log.warn("댓글(ID: {})을 찾을 수 없습니다.", commentId)
        );
    }

    public void clearAll() {
        if (!commentCache.isEmpty()) {
            commentRepository.deleteAll(commentCache.values());
            commentRepository.flush();
            commentCache.clear();
        }
    }
}
