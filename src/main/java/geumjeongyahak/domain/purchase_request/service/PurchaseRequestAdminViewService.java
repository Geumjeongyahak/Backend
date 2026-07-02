package geumjeongyahak.domain.purchase_request.service;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.classroom.service.ClassroomAdminViewService;
import geumjeongyahak.domain.file.service.ImageUploadService;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.v1.dto.request.CreatePurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.PurchaseRequestListRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestDetailResponse;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestSummaryResponse;
import geumjeongyahak.domain.vendor.service.VendorService;
import geumjeongyahak.domain.vendor.v1.dto.response.VendorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseRequestAdminViewService {

    private final PurchaseRequestService purchaseRequestService;
    private final ClassroomAdminViewService classroomAdminViewService;
    private final ImageUploadService imageUploadService;
    private final VendorService vendorService;

    public AdminPage<PurchaseRequestSummaryResponse> getPurchaseRequests(PurchaseRequestFilter filter) {
        PurchaseRequestListRequest request = new PurchaseRequestListRequest();
        request.setStatus(filter.status());
        request.setKeyword(filter.keyword());
        request.setClassroomName(filter.classroomName());
        request.setRequestedByName(filter.requestedByName());
        if (filter.page() != null) {
            request.setPage(filter.page());
        }
        if (filter.size() != null) {
            request.setSize(filter.size());
        }
        request.setSort(filter.sort());

        return AdminPage.from(purchaseRequestService.getPurchaseRequests(null, request, false));
    }

    public PurchaseRequestStatus[] getStatuses() {
        return PurchaseRequestStatus.values();
    }

    public PurchaseRequestDetailResponse getPurchaseRequest(Long requesterId, Long requestId) {
        return purchaseRequestService.getPurchaseRequest(requesterId, requestId, true);
    }

    @Transactional
    public Long createPurchaseRequest(
        Long requesterId,
        Long classroomId,
        String title,
        String content,
        List<CreatePurchaseRequestRequest.Item> items
    ) {
        return purchaseRequestService.createPurchaseRequest(
            requesterId,
            new CreatePurchaseRequestRequest(title, content, classroomId, items)
        ).id();
    }

    @Transactional
    public void updatePurchaseRequest(
        Long requesterId,
        Long requestId,
        String title,
        String content,
        List<CreatePurchaseRequestRequest.Item> items
    ) {
        purchaseRequestService.updatePurchaseRequest(
            requesterId,
            requestId,
            new CreatePurchaseRequestRequest(title, content, null, items),
            true
        );
    }

    @Transactional
    public void deletePurchaseRequest(Long requesterId, Long requestId) {
        purchaseRequestService.deletePurchaseRequest(requesterId, requestId, true);
    }

    public List<ClassroomAdminViewService.AdminClassroomRow> getAllClassrooms() {
        return classroomAdminViewService.getClassrooms(new ClassroomAdminViewService.ClassroomFilter(null, null, "name,ASC"));
    }

    public List<VendorResponse> getAllVendors() {
        return vendorService.getVendors(null);
    }

    @Transactional
    public void approve(Long approverId, Long requestId, String note) {
        purchaseRequestService.approvePurchaseRequest(approverId, requestId, note);
    }

    @Transactional
    public void reject(Long approverId, Long requestId, String note) {
        purchaseRequestService.rejectPurchaseRequest(approverId, requestId, note);
    }

    @Transactional
    public void report(
        Long requesterId,
        Long requestId,
        List<geumjeongyahak.domain.purchase_request.v1.dto.request.ReportPurchaseRequest.TransactionReport> transactions
    ) {
        purchaseRequestService.reportPurchase(requesterId, requestId, new geumjeongyahak.domain.purchase_request.v1.dto.request.ReportPurchaseRequest(transactions), true);
    }

    @Transactional
    public void confirm(Long confirmerId, Long requestId) {
        purchaseRequestService.confirmPurchase(confirmerId, requestId);
    }

    @Transactional
    public FileUploadResponse uploadReceiptImage(MultipartFile multipartFile) {
        return imageUploadService.uploadPurchaseItemImage(multipartFile);
    }

    public record PurchaseRequestFilter(
        PurchaseRequestStatus status,
        String keyword,
        String classroomName,
        String requestedByName,
        Integer page,
        Integer size,
        String sort
    ) {
    }
}
