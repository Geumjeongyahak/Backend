package geumjeongyahak.domain.purchase_request.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.purchase_request.service.ExpenseDocumentService;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestService;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestProposalService;
import geumjeongyahak.domain.purchase_request.v1.dto.request.AttachPurchaseRequestProposalReceiptRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.CreatePurchaseRequestByAdminRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.PurchaseRequestListRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.ReportPurchaseRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.ReviewPurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.SavePurchaseRequestProposalRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.UpdatePurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestDetailResponse;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestProposalResponse;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestSummaryResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/purchase-requests")
@RequiredArgsConstructor
@Tag(name = "PurchaseRequest Admin", description = "기자재 구입 요청 관리자 API")
public class PurchaseRequestAdminController {

    private final PurchaseRequestService purchaseRequestService;
    private final ExpenseDocumentService expenseDocumentService;
    private final PurchaseRequestProposalService purchaseRequestProposalService;

    @Operation(
        summary = "구입 요청 대리 생성",
        description = "관리자 또는 purchase-request:manage:* 권한자가 requestedById 사용자를 실제 요청자로 지정해 구입 요청을 생성합니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:manage:*')")
    @PostMapping
    public ResponseEntity<PurchaseRequestDetailResponse> createPurchaseRequest(
        @Valid @RequestBody CreatePurchaseRequestByAdminRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/admin/purchase-requests (requestedById={})", request.requestedById());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(purchaseRequestService.createPurchaseRequestByAdmin(userDetails.getUserId(), request));
    }

    @Operation(
        summary = "구입 요청 전체 목록 조회",
        description = "전체 분반의 구입 요청 목록을 페이지 단위로 조회합니다. "
            + "status, paymentType, keyword, classroomName, requestedByName 파라미터로 필터링할 수 있습니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:read:*')")
    @GetMapping
    public ResponseEntity<PaginationResponse<PurchaseRequestSummaryResponse>> getAllPurchaseRequests(
        @ParameterObject @Valid PurchaseRequestListRequest request
    ) {
        log.debug(
            "GET /api/v1/admin/purchase-requests (status={}, paymentType={}, keyword={}, classroomName={}, requestedByName={}, page={}, size={}, sort={})",
            request.getStatus(),
            request.getPaymentType(),
            request.getKeyword(),
            request.getClassroomName(),
            request.getRequestedByName(),
            request.getPage(),
            request.getSize(),
            request.getSort()
        );
        return ResponseEntity.ok(
            purchaseRequestService.getPurchaseRequests(null, request, false)
        );
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
        summary = "품의 정보 저장",
        description = "관리자가 CONFIRMED 이전까지 품의 정보를 중간 저장하거나 수정합니다. "
            + "모든 필드는 선택값이며 요청 본문의 전체 상태로 교체됩니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:manage:*')")
    @PutMapping("/{requestId}/proposal")
    public ResponseEntity<PurchaseRequestProposalResponse> saveProposal(
        @PathVariable Long requestId,
        @Valid @RequestBody SavePurchaseRequestProposalRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PUT /api/v1/admin/purchase-requests/{}/proposal", requestId);
        return ResponseEntity.ok(
            purchaseRequestProposalService.saveProposal(
                userDetails.getUserId(),
                requestId,
                request,
                true
            )
        );
    }

    @Operation(
        summary = "품의 단계 영수증 첨부",
        description = "관리자가 CONFIRMED 이전까지 업로드된 영수증 파일을 품의 정보에 첨부합니다. "
            + "같은 파일을 다시 요청하면 중복 첨부하지 않습니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:manage:*')")
    @PostMapping("/{requestId}/proposal/receipts")
    public ResponseEntity<PurchaseRequestProposalResponse> attachProposalReceipt(
        @PathVariable Long requestId,
        @Valid @RequestBody AttachPurchaseRequestProposalReceiptRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/admin/purchase-requests/{}/proposal/receipts", requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            purchaseRequestProposalService.attachReceipt(
                userDetails.getUserId(),
                requestId,
                request.fileId(),
                true
            )
        );
    }

    @Operation(
        summary = "품의 단계 영수증 삭제",
        description = "관리자가 CONFIRMED 이전까지 품의 단계 영수증 연결을 소프트 삭제합니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:manage:*')")
    @DeleteMapping("/{requestId}/proposal/receipts/{receiptId}")
    public ResponseEntity<Void> deleteProposalReceipt(
        @PathVariable Long requestId,
        @PathVariable Long receiptId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/admin/purchase-requests/{}/proposal/receipts/{}", requestId, receiptId);
        purchaseRequestProposalService.deleteReceipt(
            userDetails.getUserId(),
            requestId,
            receiptId,
            true
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "구입 요청 수정",
        description = "관리자 또는 purchase-request:manage:* 권한자가 PENDING 상태의 구입 요청 제목, 내용, 품목을 수정합니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:manage:*')")
    @PatchMapping("/{requestId}")
    public ResponseEntity<PurchaseRequestDetailResponse> updatePurchaseRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody UpdatePurchaseRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/admin/purchase-requests/{}", requestId);
        return ResponseEntity.ok(
            purchaseRequestService.updatePurchaseRequest(
                userDetails.getUserId(),
                requestId,
                request,
                true
            )
        );
    }

    @Operation(
        summary = "구입 요청 삭제",
        description = "PENDING 상태의 구입 요청을 소프트 삭제합니다. 이미 처리된 요청은 삭제할 수 없습니다."
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
                request.note()
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

    @Operation(
        summary = "구매 완료 보고",
        description = "관리자 또는 purchase-request:manage:* 권한자가 APPROVED 상태의 구입 요청에 구매 거래와 영수증을 등록합니다. "
            + "상태가 PURCHASED 로 변경됩니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:manage:*')")
    @PostMapping("/{requestId}/report")
    public ResponseEntity<PurchaseRequestDetailResponse> reportPurchase(
        @PathVariable Long requestId,
        @Valid @RequestBody ReportPurchaseRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/admin/purchase-requests/{}/report", requestId);
        return ResponseEntity.ok(
            purchaseRequestService.reportPurchase(userDetails.getUserId(), requestId, request, true)
        );
    }

    @Operation(summary = "구매 완료 거래 수정")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:manage:*')")
    @PatchMapping("/{requestId}/item-receipts")
    public ResponseEntity<PurchaseRequestDetailResponse> updateItemReceipts(
        @PathVariable Long requestId,
        @Valid @RequestBody ReportPurchaseRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/admin/purchase-requests/{}/item-receipts", requestId);
        return ResponseEntity.ok(
            purchaseRequestService.updateItemReceipts(userDetails.getUserId(), requestId, request, true)
        );
    }

    @Operation(
        summary = "품의서 DOCX 생성",
        description = "관리자가 미완성 품의 정보를 포함한 최신 품의서를 생성합니다. 반려된 신청은 출력할 수 없습니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:manage:*')")
    @PostMapping("/{requestId}/proposal-document")
    public ResponseEntity<Resource> generateProposalDocument(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/admin/purchase-requests/{}/proposal-document", requestId);
        return documentResponse(expenseDocumentService.generateProposal(
            userDetails.getUserId(),
            requestId,
            true
        ));
    }

    @Operation(
        summary = "결의서 DOCX 생성",
        description = "관리자가 CONFIRMED 상태의 결제 신청을 최신 실제 거래 정보로 결의서 DOCX로 생성합니다."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('purchase-request:manage:*')")
    @PostMapping("/{requestId}/resolution-document")
    public ResponseEntity<Resource> generateResolutionDocument(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/admin/purchase-requests/{}/resolution-document", requestId);
        return documentResponse(expenseDocumentService.generateResolution(
            userDetails.getUserId(),
            requestId,
            true
        ));
    }

    private ResponseEntity<Resource> documentResponse(ExpenseDocumentService.ExpenseDocumentResult document) {
        ByteArrayResource resource = new ByteArrayResource(document.content());

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(ExpenseDocumentService.DOCX_CONTENT_TYPE))
            .contentLength(document.content().length)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(document.filename(), StandardCharsets.UTF_8)
                    .build()
                    .toString()
            )
            .body(resource);
    }
}
