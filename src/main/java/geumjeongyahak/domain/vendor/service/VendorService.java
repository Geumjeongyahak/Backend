package geumjeongyahak.domain.vendor.service;

import java.util.List;
import java.util.UUID;

import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.service.FileProxyService;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import geumjeongyahak.domain.vendor.entity.Vendor;
import geumjeongyahak.domain.vendor.entity.VendorBalanceHistory;
import geumjeongyahak.domain.vendor.enums.VendorBalanceHistoryType;
import geumjeongyahak.domain.vendor.exception.VendorErrorCode;
import geumjeongyahak.domain.vendor.repository.VendorBalanceHistoryRepository;
import geumjeongyahak.domain.vendor.repository.VendorRepository;
import geumjeongyahak.domain.vendor.v1.dto.request.ChargeVendorRequest;
import geumjeongyahak.domain.vendor.v1.dto.request.CreateVendorRequest;
import geumjeongyahak.domain.vendor.v1.dto.request.UpdateVendorRequest;
import geumjeongyahak.domain.vendor.v1.dto.response.VendorBalanceHistoryResponse;
import geumjeongyahak.domain.vendor.v1.dto.response.VendorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorBalanceHistoryRepository vendorBalanceHistoryRepository;
    private final UserProxyService userProxyService;
    private final FileProxyService fileProxyService;

    public List<VendorResponse> getVendors(String keyword) {
        List<Vendor> vendors = StringUtils.hasText(keyword)
            ? vendorRepository.findAllByNameContainingIgnoreCaseOrderByNameAsc(keyword.trim())
            : vendorRepository.findAllByOrderByNameAsc();
        return vendors.stream().map(VendorResponse::from).toList();
    }

    public VendorResponse getVendor(Long vendorId) {
        return VendorResponse.from(getById(vendorId));
    }

    @Transactional
    public VendorResponse createVendor(CreateVendorRequest request) {
        Vendor vendor = vendorRepository.save(new Vendor(request.name().trim(), trimToNull(request.description())));
        return VendorResponse.from(vendor);
    }

    @Transactional
    public VendorResponse updateVendor(Long vendorId, UpdateVendorRequest request) {
        Vendor vendor = getById(vendorId);
        vendor.update(trimToNull(request.name()), trimToNull(request.description()), request.isActive());
        return VendorResponse.from(vendor);
    }

    @Transactional
    public void deleteVendor(Long vendorId) {
        vendorBalanceHistoryRepository.deleteAllByVendor_Id(vendorId);
        vendorRepository.delete(getById(vendorId));
    }

    @Transactional
    public VendorResponse chargeVendor(Long userId, Long vendorId, ChargeVendorRequest request) {
        Vendor vendor = getById(vendorId);
        User user = userProxyService.getById(userId);
        File receiptFile = getFileOrNull(request.receiptFileId());

        vendor.charge(request.amount());
        vendorBalanceHistoryRepository.save(new VendorBalanceHistory(
            vendor,
            VendorBalanceHistoryType.CHARGE,
            request.amount(),
            vendor.getBalance(),
            trimToNull(request.memo()),
            receiptFile,
            null,
            user
        ));
        return VendorResponse.from(vendor);
    }

    public List<VendorBalanceHistoryResponse> getHistories(Long vendorId) {
        getById(vendorId);
        return vendorBalanceHistoryRepository.findAllByVendor_IdOrderByOccurredAtDesc(vendorId).stream()
            .map(VendorBalanceHistoryResponse::from)
            .toList();
    }

    @Transactional
    public void deductForPurchaseRequest(Vendor vendor, PurchaseRequest purchaseRequest, User approver) {
        vendor.deduct(purchaseRequest.getTotalPrice());
        vendorBalanceHistoryRepository.save(new VendorBalanceHistory(
            vendor,
            VendorBalanceHistoryType.DEDUCT,
            purchaseRequest.getTotalPrice(),
            vendor.getBalance(),
            "결제 요청 승인 차감",
            null,
            purchaseRequest,
            approver
        ));
    }

    public Vendor getActiveById(Long vendorId) {
        Vendor vendor = getById(vendorId);
        if (!vendor.isActive()) {
            throw new geumjeongyahak.common.exception.BusinessException(VendorErrorCode.INACTIVE);
        }
        return vendor;
    }

    private Vendor getById(Long vendorId) {
        return vendorRepository.findById(vendorId)
            .orElseThrow(() -> new ResourceNotFoundException(VendorErrorCode.NOT_FOUND, vendorId));
    }

    private File getFileOrNull(UUID fileId) {
        return fileId != null ? fileProxyService.getReferenceById(fileId) : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
