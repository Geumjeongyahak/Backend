package sonmoeum.domain.file.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sonmoeum.domain.file.entity.File;

public interface FileRepository extends JpaRepository<File, UUID> {

    Optional<File> findByIdAndIsDeletedFalse(UUID id);
}
