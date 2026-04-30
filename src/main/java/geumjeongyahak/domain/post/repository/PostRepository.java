package geumjeongyahak.domain.post.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import geumjeongyahak.domain.post.entity.Post;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    // 목록 조회: channel + author 한 번에 fetch (N+1 방지)
    @EntityGraph(attributePaths = {"channel", "author"})
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    // 상세 조회: channel + author 한 번에 fetch
    @EntityGraph(attributePaths = {"channel", "author"})
    Optional<Post> findByIdAndChannelId(Long id, Long channelId);

    // 이벤트용: createdAt만 사용하므로 fetch join 불필요
    Optional<Post> findFirstByChannelIdAndIsDeletedFalseOrderByCreatedAtDescIdDesc(Long channelId);

    @Query("select p.author.id from Post p where p.id = :id and p.isDeleted = false")
    Optional<Long> findAuthorIdById(@org.springframework.data.repository.query.Param("id") Long id);
}