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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.purchase_request.service.ExpenseDocumentService;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestReconfirmationService;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestService;
import geumjeongyahak.domain.purchase_request.service.PurchaseRequestProposalService;
import geumjeongyahak.domain.purchase_request.v1.dto.request.AttachPurchaseRequestProposalReceiptRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.CreatePurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.PurchaseRequestListRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.ReportPurchaseRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.SavePurchaseRequestProposalRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.request.UpdatePurchaseRequestRequest;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestDetailResponse;
import geumjeongyahak.domain.purchase_request.v1.dto.response.PurchaseRequestProposalResponse;
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
    private final PurchaseRequestProposalService purchaseRequestProposalService;
    private final ExpenseDocumentService expenseDocumentService;

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "구입 요청 생성",
        description = "필수값인 classroomId와 departmentId로 담당 분반과 부서를 직접 지정하여 기자재 구입 요청을 생성합니다. "
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
        description = "구입 요청 목록을 페이지 단위로 조회합니다. 기본 목록은 전체 요청을 반환하며, mine=true 파라미터를 전달하면 본인이 신청한 요청만 반환합니다. "
            + "status, paymentType, keyword, classroomName, requestedByName 파라미터로 필터링할 수 있습니다."
    )
    @GetMapping
    public ResponseEntity<PaginationResponse<PurchaseRequestSummaryResponse>> getPurchaseRequests(
        @ParameterObject @Valid PurchaseRequestListRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug(
            "GET /api/v1/purchase-requests (status={}, paymentType={}, mine={}, keyword={}, classroomName={}, requestedByName={}, page={}, size={}, sort={})",
            request.getStatus(),
            request.getPaymentType(),
            request.isMine(),
            request.getKeyword(),
            request.getClassroomName(),
            request.getRequestedByName(),
            request.getPage(),
            request.getSize(),
            request.getSort()
        );
        return ResponseEntity.ok(
            purchaseRequestService.getPurchaseRequests(
                userDetails.getUserId(),
                request,
                request.isMine()
            )
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
        summary = "구입 요청 수정",
        description = "작성자가 본인의 PENDING 상태 구입 요청 분반, 담당 부서, 제목, 내용, 품목을 전체 교체합니다."
    )
    @PutMapping("/{requestId}")
    public ResponseEntity<PurchaseRequestDetailResponse> updatePurchaseRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody UpdatePurchaseRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PUT /api/v1/purchase-requests/{}", requestId);
        return ResponseEntity.ok(
            purchaseRequestService.updatePurchaseRequest(
                userDetails.getUserId(),
                requestId,
                request,
                false
            )
        );
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "품의 정보 저장",
        description = "최초 작성자가 CONFIRMED 이전까지 품의 정보를 중간 저장하거나 수정합니다. "
            + "모든 필드는 선택값이며 요청 본문의 전체 상태로 교체됩니다."
    )
    @PutMapping("/{requestId}/proposal")
    public ResponseEntity<PurchaseRequestProposalResponse> saveProposal(
        @PathVariable Long requestId,
        @Valid @RequestBody SavePurchaseRequestProposalRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PUT /api/v1/purchase-requests/{}/proposal", requestId);
        return ResponseEntity.ok(
            purchaseRequestProposalService.saveProposal(
                userDetails.getUserId(),
                requestId,
                request,
                false
            )
        );
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "품의 단계 영수증 첨부",
        description = "최초 작성자가 CONFIRMED 이전까지 업로드된 영수증 파일을 품의 정보에 첨부합니다. "
            + "같은 파일을 다시 요청하면 중복 첨부하지 않습니다."
    )
    @PostMapping("/{requestId}/proposal/receipts")
    public ResponseEntity<PurchaseRequestProposalResponse> attachProposalReceipt(
        @PathVariable Long requestId,
        @Valid @RequestBody AttachPurchaseRequestProposalReceiptRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/purchase-requests/{}/proposal/receipts", requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            purchaseRequestProposalService.attachReceipt(
                userDetails.getUserId(),
                requestId,
                request.fileId(),
                false
            )
        );
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "품의 단계 영수증 삭제",
        description = "최초 작성자가 CONFIRMED 이전까지 품의 단계 영수증 연결을 소프트 삭제합니다."
    )
    @DeleteMapping("/{requestId}/proposal/receipts/{receiptId}")
    public ResponseEntity<Void> deleteProposalReceipt(
        @PathVariable Long requestId,
        @PathVariable Long receiptId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/purchase-requests/{}/proposal/receipts/{}", requestId, receiptId);
        purchaseRequestProposalService.deleteReceipt(
            userDetails.getUserId(),
            requestId,
            receiptId,
            false
        );
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "품의서 DOCX 생성",
        description = "최초 작성자가 미완성 품의 정보를 포함한 최신 품의서를 생성합니다. 반려된 신청은 출력할 수 없습니다."
    )
    @PostMapping("/{requestId}/proposal-document")
    public ResponseEntity<Resource> generateProposalDocument(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/purchase-requests/{}/proposal-document", requestId);
        return documentResponse(expenseDocumentService.generateProposal(
            userDetails.getUserId(),
            requestId,
            false
        ));
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "결의서 DOCX 생성",
        description = "최초 작성자가 CONFIRMED 상태의 결제 신청을 최신 실제 거래 정보로 결의서 DOCX로 생성합니다."
    )
    @PostMapping("/{requestId}/resolution-document")
    public ResponseEntity<Resource> generateResolutionDocument(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/purchase-requests/{}/resolution-document", requestId);
        return documentResponse(expenseDocumentService.generateResolution(
            userDetails.getUserId(),
            requestId,
            false
        ));
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "구입 요청 삭제",
        description = "본인이 작성한 PENDING 상태의 구입 요청을 소프트 삭제합니다. "
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
