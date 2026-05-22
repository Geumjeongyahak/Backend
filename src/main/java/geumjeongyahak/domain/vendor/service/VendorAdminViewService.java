package geumjeongyahak.domain.vendor.service;

import geumjeongyahak.domain.base.dto.response.AdminSorts;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

import geumjeongyahak.domain.vendor.v1.dto.request.ChargeVendorRequest;
import geumjeongyahak.domain.vendor.v1.dto.request.CreateVendorRequest;
import geumjeongyahak.domain.vendor.v1.dto.request.UpdateVendorRequest;
import geumjeongyahak.domain.vendor.v1.dto.response.VendorBalanceHistoryResponse;
import geumjeongyahak.domain.vendor.v1.dto.response.VendorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorAdminViewService {

    private final VendorService vendorService;

    public List<VendorResponse> getVendors(String keyword, boolean activeOnly, String sort) {
        List<VendorResponse> vendors = vendorService.getVendors(keyword).stream()
            .filter(vendor -> !activeOnly || vendor.isActive())
            .toList();

        return AdminSorts.sort(vendors, sort, Map.of(
            "id", Comparator.comparing(VendorResponse::id),
            "name", Comparator.comparing(VendorResponse::name, Comparator.nullsLast(String::compareToIgnoreCase)),
            "balance", Comparator.comparing(VendorResponse::balance, Comparator.nullsLast(Long::compareTo)),
            "status", Comparator.comparing(VendorResponse::isActive),
            "createdAt", Comparator.comparing(VendorResponse::createdAt, Comparator.nullsLast(java.time.LocalDateTime::compareTo))
        ), "name,ASC");
    }

    public VendorResponse getVendor(Long vendorId) {
        return vendorService.getVendor(vendorId);
    }

    public List<VendorBalanceHistoryResponse> getHistories(Long vendorId) {
        return vendorService.getHistories(vendorId);
    }

    @Transactional
    public Long createVendor(String name, String description) {
        return vendorService.createVendor(new CreateVendorRequest(name, description)).id();
    }

    @Transactional
    public void updateVendor(Long vendorId, String name, String description, Boolean isActive) {
        vendorService.updateVendor(vendorId, new UpdateVendorRequest(name, description, isActive));
    }

    @Transactional
    public void chargeVendor(Long userId, Long vendorId, Long amount, String memo, UUID receiptFileId) {
        vendorService.chargeVendor(userId, vendorId, new ChargeVendorRequest(amount, memo, receiptFileId));
    }

    @Transactional
    public void deleteVendor(Long vendorId) {
        vendorService.deleteVendor(vendorId);
    }
}
