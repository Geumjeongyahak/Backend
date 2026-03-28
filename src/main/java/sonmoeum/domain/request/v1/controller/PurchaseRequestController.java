package sonmoeum.domain.request.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sonmoeum.common.security.service.CustomUserDetails;
import sonmoeum.domain.request.enums.RequestStatus;
import sonmoeum.domain.request.service.PurchaseRequestService;
import sonmoeum.domain.request.v1.dto.request.CreatePurchaseRequestRequest;
import sonmoeum.domain.request.v1.dto.request.RejectRequestRequest;
import sonmoeum.domain.request.v1.dto.response.PurchaseRequestResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/purchase-requests")
@RequiredArgsConstructor
@Tag(name = "PurchaseRequest", description = "기자재 구입 요청 API")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "구입 요청 생성", description = "기자재 구입 요청을 생성합니다.")
    @PostMapping
    public ResponseEntity<PurchaseRequestResponse> createPurchaseRequest(
        @Valid @RequestBody CreatePurchaseRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/purchase-requests - 구입 요청 생성 (subjectId={})", request.subjectId());
        PurchaseRequestResponse response = purchaseRequestService.createPurchaseRequest(
            userDetails.getUserId(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "구입 요청 목록 조회", description = "구입 요청 목록을 조회합니다. 관리자는 전체, 일반 사용자는 본인 요청만 조회됩니다.")
    @GetMapping
    public ResponseEntity<List<PurchaseRequestResponse>> getPurchaseRequests(
        @RequestParam(required = false) RequestStatus status,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/purchase-requests - 구입 요청 목록 조회 (status={})", status);
        List<PurchaseRequestResponse> response = purchaseRequestService.getPurchaseRequests(
            userDetails.getUserId(), userDetails.isAdmin(), status
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "구입 요청 상세 조회", description = "구입 요청 상세 정보를 조회합니다.")
    @GetMapping("/{requestId}")
    public ResponseEntity<PurchaseRequestResponse> getPurchaseRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/purchase-requests/{} - 구입 요청 상세 조회", requestId);
        PurchaseRequestResponse response = purchaseRequestService.getPurchaseRequest(
            userDetails.getUserId(), requestId, userDetails.isAdmin()
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "구입 요청 승인", description = "구입 요청을 승인합니다.")
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<PurchaseRequestResponse> approvePurchaseRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/purchase-requests/{}/approve - 구입 요청 승인", requestId);
        PurchaseRequestResponse response = purchaseRequestService.approvePurchaseRequest(
            userDetails.getUserId(), requestId
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "구입 요청 반려", description = "구입 요청을 반려합니다.")
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<PurchaseRequestResponse> rejectPurchaseRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody RejectRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/purchase-requests/{}/reject - 구입 요청 반려", requestId);
        PurchaseRequestResponse response = purchaseRequestService.rejectPurchaseRequest(
            userDetails.getUserId(), requestId, request.note()
        );
        return ResponseEntity.ok(response);
    }
}
