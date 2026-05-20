package geumjeongyahak.domain.file.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import geumjeongyahak.domain.file.entity.File;

public interface FileRepository extends JpaRepository<File, UUID> {

    Optional<File> findByIdAndIsDeletedFalse(UUID id);

    List<File> findAllByIdInAndIsDeletedFalse(Collection<UUID> ids);

    List<File> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime threshold);

    @Query("""
        select f
        from File f
        where f.isDeleted = false
          and f.createdAt < :threshold
          and f.storageKey like concat(:storageKeyPrefix, '%')
          and not exists (
              select 1
              from PurchaseRequestItem item
              where item.receiptFile = f
          )
          and not exists (
              select 1
              from PurchaseRequestReceipt receipt
              where receipt.file = f
          )
        """)
    List<File> findUnlinkedPurchaseItemFilesBefore(
        @Param("storageKeyPrefix") String storageKeyPrefix,
        @Param("threshold") LocalDateTime threshold
    );
}
