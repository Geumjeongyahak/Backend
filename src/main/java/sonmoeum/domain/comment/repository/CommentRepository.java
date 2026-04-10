package sonmoeum.domain.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sonmoeum.domain.comment.entity.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByPostIdAndIsDeletedFalseOrderByCreatedAtAscIdAsc(Long postId);

    Optional<Comment> findByIdAndPostId(Long commentId, Long postId);
}
