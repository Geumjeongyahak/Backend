package geumjeongyahak.domain.post.repository;

import geumjeongyahak.domain.post.entity.PostAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {

    boolean existsByPostIdAndFileId(Long postId, UUID fileId);

    long countByPostId(Long postId);

    Optional<PostAttachment> findByPostIdAndFileId(Long postId, UUID fileId);
}
