package geumjeongyahak.domain.post.repository;

import geumjeongyahak.domain.post.entity.PostAttachment;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {

    boolean existsByPostIdAndFileId(Long postId, UUID fileId);

    long countByPostId(Long postId);

    Optional<PostAttachment> findByPostIdAndFileId(Long postId, UUID fileId);

    @Query("SELECT pa.file.id FROM PostAttachment pa WHERE pa.post.id = :postId")
    List<UUID> findFileIdsByPostId(@Param("postId") Long postId);

    @Query("SELECT DISTINCT pa.file.id FROM PostAttachment pa WHERE pa.file.id IN :fileIds AND pa.post.id <> :postId")
    List<UUID> findReferencedFileIdsByFileIdInAndPostIdNot(
            @Param("fileIds") Collection<UUID> fileIds,
            @Param("postId") Long postId
    );

    @Modifying
    @Query("DELETE FROM PostAttachment pa WHERE pa.file.id = :fileId")
    void deleteByFileId(@Param("fileId") UUID fileId);
}
