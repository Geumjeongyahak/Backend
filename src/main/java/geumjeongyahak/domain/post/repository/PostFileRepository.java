package geumjeongyahak.domain.post.repository;

import geumjeongyahak.domain.post.entity.PostFile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostFileRepository extends JpaRepository<PostFile, Long> {

    boolean existsByPostIdAndFileId(Long postId, UUID fileId);

    long countByPostId(Long postId);

    Optional<PostFile> findFirstByPostIdOrderBySortOrderAsc(Long postId);

    @Query("SELECT pf FROM PostFile pf JOIN FETCH pf.file WHERE pf.post.id = :postId ORDER BY pf.sortOrder ASC")
    List<PostFile> findAllByPostIdWithFileOrderBySortOrderAsc(@Param("postId") Long postId);

    @Query("SELECT pf.file.id FROM PostFile pf WHERE pf.post.id = :postId")
    List<UUID> findFileIdsByPostId(@Param("postId") Long postId);

    @Query("SELECT DISTINCT pf.file.id FROM PostFile pf WHERE pf.file.id IN :fileIds AND pf.post.id <> :postId")
    List<UUID> findReferencedFileIdsByFileIdInAndPostIdNot(
            @Param("fileIds") Collection<UUID> fileIds,
            @Param("postId") Long postId
    );

    @Modifying
    @Query("DELETE FROM PostFile pf WHERE pf.file.id = :fileId")
    void deleteByFileId(@Param("fileId") UUID fileId);
}
