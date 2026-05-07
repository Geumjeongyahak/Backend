package geumjeongyahak.domain.purchase_request.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestService;
import geumjeongyahak.domain.purchase_request.v1.dto.request.ReviewPurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestDetailResponse;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestSummaryResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/purchase-requests")
@RequiredArgsConstructor
@Tag(name = "PurchaseRequest Admin", description = "기자재 구입 요청 관리자 API")
public class PurchaseRequestAdminController {

    private final PurchaseRequestService purchaseRequestService;

    @Operation(
        summary = "구입 요청 전체 목록 조회",
        description = "전체 분반의 구입 요청 목록을 조회합니다. status 파라미터로 필터링할 수 있습니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:read:*')")
    @GetMapping
    public ResponseEntity<List<PurchaseRequestSummaryResponse>> getAllPurchaseRequests(
        @RequestParam(required = false) PurchaseRequestStatus status
    ) {
        log.debug("GET /api/v1/admin/purchase-requests (status={})", status);
        return ResponseEntity.ok(purchaseRequestService.getAllPurchaseRequests(status));
    }

    @Operation(summary = "구입 요청 상세 조회")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasAuthority('purchase-request:read:*')")
    @GetMapping("/{requestId}")
    public ResponseEntity<PurchaseRequestDetailResponse> getPurchaseRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/admin/purchase-requests/{}", requestId);
        return ResponseEntity.ok(
            purchaseRequestService.getPurchaseRequest(userDetails.getUserId(), requestId, true)
        );
    }

    @Operation(
        summary = "구입 요청 삭제",
        description = "PENDING 상태의 구입 요청을 삭제합니다. 이미 처리된 요청은 삭제할 수 없습니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:manage:*')")
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deletePurchaseRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/admin/purchase-requests/{}", requestId);
        purchaseRequestService.deletePurchaseRequest(userDetails.getUserId(), requestId, true);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "구입 요청 승인",
        description = "PENDING 상태의 구입 요청을 승인합니다. 승인 후 7일 이내에 구매 완료 보고가 이루어져야 합니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:review:*')")
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<PurchaseRequestDetailResponse> approvePurchaseRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody ReviewPurchaseRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/admin/purchase-requests/{}/approve", requestId);
        return ResponseEntity.ok(
            purchaseRequestService.approvePurchaseRequest(
                userDetails.getUserId(),
                requestId,
                request.note(),
                request.advancePaymentApprovedAmount()
            )
        );
    }

    @Operation(summary = "구입 요청 반려")
    @PreAuthorize("hasRole('ADMIN') or  hasAuthority('purchase-request:review:*')")
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<PurchaseRequestDetailResponse> rejectPurchaseRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody ReviewPurchaseRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/admin/purchase-requests/{}/reject", requestId);
        return ResponseEntity.ok(
            purchaseRequestService.rejectPurchaseRequest(userDetails.getUserId(), requestId, request.note())
        );
    }

    @Operation(
        summary = "결재 확인",
        description = "PURCHASED 상태의 요청에 대해 영수증을 확인하고 최종 결재 완료 처리합니다. "
            + "상태가 CONFIRMED 로 변경됩니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:manage:*')")
    @PatchMapping("/{requestId}/confirm")
    public ResponseEntity<PurchaseRequestDetailResponse> confirmPurchase(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/admin/purchase-requests/{}/confirm", requestId);
        return ResponseEntity.ok(
            purchaseRequestService.confirmPurchase(userDetails.getUserId(), requestId)
        );
    }
}
