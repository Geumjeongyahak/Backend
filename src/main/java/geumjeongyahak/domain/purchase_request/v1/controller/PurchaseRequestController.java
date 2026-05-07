package geumjeongyahak.domain.purchase_request.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestItemService;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestReconfirmationService;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestService;
import geumjeongyahak.domain.purchase_request.v1.dto.request.CreatePurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.ReportPurchaseRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestDetailResponse;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestSummaryResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/classrooms/{classroomId}/purchase-requests")
@RequiredArgsConstructor
@Tag(name = "PurchaseRequest", description = "기자재 구입 요청 API")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;
    private final PurchaseRequestItemService purchaseRequestItemService;
    private final PurchaseRequestReconfirmationService purchaseRequestReconfirmationService;

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "구입 요청 생성",
        description = "분반에 대한 기자재 구입 요청을 생성합니다. "
            + "품목은 품명과 사유만 입력하며 가격은 승인 후 구매 완료 보고 시점에 입력합니다. "
            + "상태는 PENDING 으로 시작합니다."
    )
    @PostMapping
    public ResponseEntity<PurchaseRequestDetailResponse> createPurchaseRequest(
        @PathVariable Long classroomId,
        @Valid @RequestBody CreatePurchaseRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/classrooms/{}/purchase-requests", classroomId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(purchaseRequestService.createPurchaseRequest(userDetails.getUserId(), classroomId, request));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "구입 요청 목록 조회",
        description = "본인이 신청한 구입 요청 목록을 조회합니다. status 파라미터로 필터링할 수 있습니다."
    )
    @GetMapping
    public ResponseEntity<List<PurchaseRequestSummaryResponse>> getPurchaseRequests(
        @PathVariable Long classroomId,
        @RequestParam(required = false) PurchaseRequestStatus status,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/classrooms/{}/purchase-requests (status={})", classroomId, status);
        return ResponseEntity.ok(
            purchaseRequestService.getPurchaseRequests(userDetails.getUserId(), status)
        );
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "구입 요청 상세 조회")
    @GetMapping("/{requestId}")
    public ResponseEntity<PurchaseRequestDetailResponse> getPurchaseRequest(
        @PathVariable Long classroomId,
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/classrooms/{}/purchase-requests/{}", classroomId, requestId);
        return ResponseEntity.ok(
            purchaseRequestService.getPurchaseRequest(userDetails.getUserId(), requestId, false)
        );
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "구입 요청 삭제",
        description = "본인이 작성한 PENDING 상태의 구입 요청을 삭제합니다. "
            + "이미 처리된 요청은 이력 보존을 위해 삭제할 수 없습니다."
    )
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deletePurchaseRequest(
        @PathVariable Long classroomId,
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/classrooms/{}/purchase-requests/{}", classroomId, requestId);
        purchaseRequestService.deletePurchaseRequest(userDetails.getUserId(), requestId, false);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "구매 완료 보고",
        description = "APPROVED 상태에서 승인 후 7일 이내에 실제 구매 금액과 영수증을 제출합니다. "
            + "상태가 PURCHASED 로 변경됩니다."
    )
    @PostMapping("/{requestId}/report")
    public ResponseEntity<PurchaseRequestDetailResponse> reportPurchase(
        @PathVariable Long classroomId,
        @PathVariable Long requestId,
        @Valid @RequestBody ReportPurchaseRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/classrooms/{}/purchase-requests/{}/report", classroomId, requestId);
        return ResponseEntity.ok(
            purchaseRequestService.reportPurchase(userDetails.getUserId(), requestId, request, false)
        );
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "구입 요청 재확인 요청",
        description = "PURCHASED 상태의 구입 요청에 대해 결재 확인 재확인을 요청합니다. "
            + "현재는 알림 서비스 연동 전 임시 엔드포인트이며 상태를 변경하지 않습니다."
    )
    @PostMapping("/{requestId}/reconfirmation")
    public ResponseEntity<Void> requestReconfirmation(
        @PathVariable Long classroomId,
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/classrooms/{}/purchase-requests/{}/reconfirmation", classroomId, requestId);
        purchaseRequestReconfirmationService.requestReconfirmation(userDetails.getUserId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "구입 항목 영수증 업로드",
        description = "특정 구입 항목에 대한 영수증 이미지를 업로드하고 해당 항목에 연결합니다."
    )
    @PostMapping(value = "/{requestId}/items/{itemId}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadItemReceipt(
        @PathVariable Long classroomId,
        @PathVariable Long requestId,
        @PathVariable Long itemId,
        @RequestPart("file") MultipartFile file,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/classrooms/{}/purchase-requests/{}/items/{}/receipt", classroomId, requestId, itemId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(purchaseRequestItemService.uploadItemReceipt(userDetails.getUserId(), requestId, itemId, file, userDetails.isAdminOrManager()));
    }
}
