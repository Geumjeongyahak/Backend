package geumjeongyahak.domain.vendor.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import geumjeongyahak.domain.vendor.entity.Vendor;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    List<Vendor> findAllByIsDeletedFalseOrderByNameAsc();

    List<Vendor> findAllByNameContainingIgnoreCaseAndIsDeletedFalseOrderByNameAsc(String name);

    Optional<Vendor> findByIdAndIsDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vendor v where v.id = :id and v.isDeleted = false")
    Optional<Vendor> findByIdForUpdate(@Param("id") Long id);
}
