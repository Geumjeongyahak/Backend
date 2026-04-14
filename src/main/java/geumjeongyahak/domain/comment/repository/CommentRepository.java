package geumjeongyahak.domain.comment.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import geumjeongyahak.domain.comment.entity.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 목록 조회: author(이름 접근) + parentComment(ID 접근) fetch join
    @EntityGraph(attributePaths = {"author", "parentComment"})
    List<Comment> findAllByPostIdAndIsDeletedFalseOrderByCreatedAtAscIdAsc(Long postId);

    // 단건 조회: author(권한 검사) + parentComment(대댓글 깊이 검사) fetch join
    @EntityGraph(attributePaths = {"author", "parentComment"})
    Optional<Comment> findByIdAndPostId(Long commentId, Long postId);
}