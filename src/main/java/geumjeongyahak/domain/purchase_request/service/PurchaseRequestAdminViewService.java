package geumjeongyahak.domain.purchase_request.service;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.AdminSorts;
import geumjeongyahak.domain.classroom.service.ClassroomAdminViewService;
import geumjeongyahak.domain.file.service.ImageUploadService;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.purchase_request.enums.PurchasePaymentMethod;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.v1.dto.request.CreatePurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestDetailResponse;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestSummaryResponse;
import geumjeongyahak.domain.vendor.service.VendorService;
import geumjeongyahak.domain.vendor.v1.dto.response.VendorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseRequestAdminViewService {

    private final PurchaseRequestService purchaseRequestService;
    private final ClassroomAdminViewService classroomAdminViewService;
    private final ImageUploadService imageUploadService;
    private final VendorService vendorService;

    public AdminPage<PurchaseRequestSummaryResponse> getPurchaseRequests(PurchaseRequestFilter filter) {
        List<PurchaseRequestSummaryResponse> rows = purchaseRequestService.getAllPurchaseRequests(filter.status())
            .stream()
            .filter(request -> matchesKeyword(request, filter.keyword()))
            .filter(request -> isBlank(filter.classroomName())
                || contains(request.classroomName(), filter.classroomName().trim().toLowerCase(Locale.ROOT)))
            .filter(request -> isBlank(filter.requestedByName())
                || contains(request.requestedByName(), filter.requestedByName().trim().toLowerCase(Locale.ROOT)))
            .toList();

        return AdminPage.from(sortPurchaseRequests(rows, filter.sort()), filter.page(), filter.size());
    }

    private List<PurchaseRequestSummaryResponse> sortPurchaseRequests(List<PurchaseRequestSummaryResponse> rows, String sort) {
        return AdminSorts.sort(rows, sort, Map.of(
            "id", Comparator.comparing(PurchaseRequestSummaryResponse::id),
            "title", Comparator.comparing(PurchaseRequestSummaryResponse::title, Comparator.nullsLast(String::compareToIgnoreCase)),
            "classroomName", Comparator.comparing(PurchaseRequestSummaryResponse::classroomName, Comparator.nullsLast(String::compareToIgnoreCase)),
            "requestedByName", Comparator.comparing(PurchaseRequestSummaryResponse::requestedByName, Comparator.nullsLast(String::compareToIgnoreCase)),
            "totalPrice", Comparator.comparing(PurchaseRequestSummaryResponse::totalPrice, Comparator.nullsLast(Long::compareTo)),
            "status", Comparator.comparing(response -> response.status().name()),
            "createdAt", Comparator.comparing(PurchaseRequestSummaryResponse::createdAt, Comparator.nullsLast(LocalDateTime::compareTo))
        ), "createdAt,DESC");
    }

    private boolean matchesKeyword(PurchaseRequestSummaryResponse request, String keyword) {
        if (isBlank(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(request.title(), normalized)
            || contains(request.classroomName(), normalized)
            || contains(request.requestedByName(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
        PurchasePaymentMethod paymentMethod,
        Long vendorId,
        List<CreatePurchaseRequestRequest.Item> items
    ) {
        return purchaseRequestService.createPurchaseRequest(
            requesterId,
            new CreatePurchaseRequestRequest(title, content, classroomId, paymentMethod, vendorId, items)
        ).id();
    }

    @Transactional
    public void updatePurchaseRequest(
        Long requesterId,
        Long requestId,
        String title,
        String content,
        PurchasePaymentMethod paymentMethod,
        Long vendorId,
        List<CreatePurchaseRequestRequest.Item> items
    ) {
        purchaseRequestService.updatePurchaseRequest(
            requesterId,
            requestId,
            new CreatePurchaseRequestRequest(title, content, null, paymentMethod, vendorId, items),
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
        List<geumjeongyahak.domain.purchase_request.v1.dto.request.ReportPurchaseRequest.ItemReport> items
    ) {
        purchaseRequestService.reportPurchase(requesterId, requestId, new geumjeongyahak.domain.purchase_request.v1.dto.request.ReportPurchaseRequest(items), true);
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
