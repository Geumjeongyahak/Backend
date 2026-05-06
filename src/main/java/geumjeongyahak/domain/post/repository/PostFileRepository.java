package geumjeongyahak.domain.post.repository;

import geumjeongyahak.domain.post.entity.PostFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostFileRepository extends JpaRepository<PostFile, Long> {

    boolean existsByPostIdAndFileId(Long postId, UUID fileId);

    long countByPostId(Long postId);

    Optional<PostFile> findFirstByPostIdOrderBySortOrderAsc(Long postId);
}
