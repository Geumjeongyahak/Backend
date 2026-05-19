package geumjeongyahak.domain.post.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.enums.PostStatus;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    // 목록 조회: channel + author 한 번에 fetch (N+1 방지)
    @EntityGraph(attributePaths = {"channel", "author"})
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    // 상세 조회: channel + author 한 번에 fetch
    @EntityGraph(attributePaths = {"channel", "author"})
    Optional<Post> findByIdAndChannelId(Long id, Long channelId);

    // 상세 응답용: attachments + file까지 한 번에 fetch (N+1 방지)
    @EntityGraph(attributePaths = {"channel", "author", "postAttachments", "postAttachments.file"})
    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.channel.id = :channelId")
    Optional<Post> findWithAttachmentsByIdAndChannelId(@Param("id") Long id, @Param("channelId") Long channelId);

    // 이벤트용: createdAt만 사용하므로 fetch join 불필요
    Optional<Post> findFirstByChannelIdAndStatusAndIsDeletedFalseOrderByCreatedAtDescIdDesc(Long channelId, PostStatus status);

    // 내 초안 목록 조회
    @EntityGraph(attributePaths = {"channel", "author"})
    Page<Post> findAllByChannelIdAndAuthorIdAndStatusAndIsDeletedFalse(Long channelId, Long authorId, PostStatus status, Pageable pageable);

    @Query("select p.author.id from Post p where p.id = :id and p.isDeleted = false")
    Optional<Long> findAuthorIdById(@org.springframework.data.repository.query.Param("id") Long id);
}
