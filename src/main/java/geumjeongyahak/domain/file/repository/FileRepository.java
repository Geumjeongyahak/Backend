package geumjeongyahak.domain.file.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import geumjeongyahak.domain.file.entity.File;

public interface FileRepository extends JpaRepository<File, UUID> {

    Optional<File> findByIdAndIsDeletedFalse(UUID id);

    List<File> findAllByIdInAndIsDeletedFalse(Collection<UUID> ids);

    List<File> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime threshold);
}
