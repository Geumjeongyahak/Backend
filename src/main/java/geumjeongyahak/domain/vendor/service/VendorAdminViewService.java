package geumjeongyahak.domain.vendor.service;

import java.util.List;
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

    public List<VendorResponse> getVendors(String keyword) {
        return vendorService.getVendors(keyword);
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
