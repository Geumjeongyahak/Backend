package geumjeongyahak.domain.vendor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import geumjeongyahak.domain.vendor.entity.Vendor;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    List<Vendor> findAllByOrderByNameAsc();

    List<Vendor> findAllByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
