package geumjeongyahak.domain.purchase_request.v1.controller;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestReconfirmationService;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestService;
import geumjeongyahak.domain.purchase_request.v1.dto.request.CreatePurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.ReportPurchaseRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestDetailResponse;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestSummaryResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/purchase-requests")
@RequiredArgsConstructor
@Tag(name = "PurchaseRequest", description = "기자재 구입 요청 API")
public class PurchaseRequestController {

    private static final String TEACHER_OR_HIGHER_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN')";

    private final PurchaseRequestService purchaseRequestService;
    private final PurchaseRequestReconfirmationService purchaseRequestReconfirmationService;

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "구입 요청 생성",
        description = "classroomId 로 지정한 분반에 대한 기자재 구입 요청을 생성합니다. "
            + "품목은 품명, 사유, 확정 결제금액, 선택 영수증을 입력합니다. "
            + "상태는 PENDING 으로 시작합니다."
    )
    @PostMapping
    public ResponseEntity<PurchaseRequestDetailResponse> createPurchaseRequest(
        @Valid @RequestBody CreatePurchaseRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/purchase-requests (classroomId={})", request.classroomId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(purchaseRequestService.createPurchaseRequest(userDetails.getUserId(), request));
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "구입 요청 목록 조회",
        description = "구입 요청 목록을 조회합니다. 기본 목록은 전체 요청을 반환하며, mine=true 파라미터를 전달하면 본인이 신청한 요청만 반환합니다. "
            + "status 파라미터로 필터링할 수 있습니다."
    )
    @GetMapping
    public ResponseEntity<List<PurchaseRequestSummaryResponse>> getPurchaseRequests(
        @RequestParam(required = false) PurchaseRequestStatus status,
        @RequestParam(defaultValue = "false") boolean mine,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/purchase-requests (status={}, mine={})", status, mine);
        return ResponseEntity.ok(
            purchaseRequestService.getPurchaseRequests(userDetails.getUserId(), status, mine)
        );
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "구입 요청 상세 조회",
        description = "인증된 사용자가 구입 요청 단건 상세 정보를 조회합니다. "
            + "목록 조회와 동일하게 기본 상세 조회는 전체 요청에 대해 허용됩니다."
    )
    @GetMapping("/{requestId}")
    public ResponseEntity<PurchaseRequestDetailResponse> getPurchaseRequest(
        @PathVariable Long requestId
    ) {
        log.debug("GET /api/v1/purchase-requests/{}", requestId);
        return ResponseEntity.ok(
            purchaseRequestService.getPurchaseRequest(requestId)
        );
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "구입 요청 삭제",
        description = "본인이 작성한 PENDING 상태의 구입 요청을 삭제합니다. "
            + "이미 처리된 요청은 이력 보존을 위해 삭제할 수 없습니다."
    )
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deletePurchaseRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/purchase-requests/{}", requestId);
        purchaseRequestService.deletePurchaseRequest(userDetails.getUserId(), requestId, false);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "구매 완료 보고",
        description = "APPROVED 상태에서 승인 후 7일 이내에 실제 구매 금액과 영수증을 제출합니다. "
            + "상태가 PURCHASED 로 변경됩니다."
    )
    @PostMapping("/{requestId}/report")
    public ResponseEntity<PurchaseRequestDetailResponse> reportPurchase(
        @PathVariable Long requestId,
        @Valid @RequestBody ReportPurchaseRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/purchase-requests/{}/report", requestId);
        return ResponseEntity.ok(
            purchaseRequestService.reportPurchase(userDetails.getUserId(), requestId, request, false)
        );
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "구입 요청 재확인 요청",
        description = "PURCHASED 상태의 구입 요청에 대해 결재 확인 재확인을 요청합니다. "
            + "현재는 알림 서비스 연동 전 임시 엔드포인트이며 상태를 변경하지 않습니다."
    )
    @PostMapping("/{requestId}/reconfirmation")
    public ResponseEntity<Void> requestReconfirmation(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/purchase-requests/{}/reconfirmation", requestId);
        purchaseRequestReconfirmationService.requestReconfirmation(userDetails.getUserId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(summary = "구매 완료 거래 수정")
    @PostMapping("/{requestId}/item-receipts")
    public ResponseEntity<PurchaseRequestDetailResponse> updateItemReceipts(
        @PathVariable Long requestId,
        @Valid @RequestBody ReportPurchaseRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/purchase-requests/{}/item-receipts", requestId);
        return ResponseEntity.ok(
            purchaseRequestService.updateItemReceipts(userDetails.getUserId(), requestId, request, false)
        );
    }

}
