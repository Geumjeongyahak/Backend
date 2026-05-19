package geumjeongyahak.domain.vendor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.domain.vendor.entity.VendorBalanceHistory;

public interface VendorBalanceHistoryRepository extends JpaRepository<VendorBalanceHistory, Long> {

    List<VendorBalanceHistory> findAllByVendor_IdOrderByOccurredAtDesc(Long vendorId);

    @Transactional
    void deleteAllByVendor_Id(Long vendorId);
}
