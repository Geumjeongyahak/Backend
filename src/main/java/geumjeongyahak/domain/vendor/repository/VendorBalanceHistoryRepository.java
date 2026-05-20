package geumjeongyahak.domain.vendor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.domain.vendor.entity.VendorBalanceHistory;

public interface VendorBalanceHistoryRepository extends JpaRepository<VendorBalanceHistory, Long> {

    List<VendorBalanceHistory> findAllByVendor_IdAndIsDeletedFalseOrderByOccurredAtDesc(Long vendorId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VendorBalanceHistory h set h.isDeleted = true, h.deletedAt = current_timestamp where h.vendor.id = :vendorId")
    void softDeleteAllByVendor_Id(@Param("vendorId") Long vendorId);

    @Transactional
    void deleteAllByVendor_Id(Long vendorId);
}
