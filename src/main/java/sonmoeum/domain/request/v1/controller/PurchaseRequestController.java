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
    @Operation(
        summary = "구입 요청 생성",
        description = "인증된 사용자가 특정 과목에 대한 기자재 구입 요청을 생성합니다. "
            + "요청 생성 시 제목, 내용, 금액, 요청자, 대상 과목이 함께 저장되며 상태는 PENDING 으로 시작합니다. "
            + "생성 단계에서는 예산 처리나 과목 데이터 변경 같은 side effect 는 발생하지 않습니다."
    )
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
    @Operation(
        summary = "구입 요청 목록 조회",
        description = "구입 요청 목록을 조회합니다. ADMIN 과 MANAGER 는 전체 요청을 조회할 수 있고, "
            + "일반 사용자는 본인이 생성한 요청만 조회할 수 있습니다. "
            + "status 파라미터를 전달하면 해당 상태의 요청만 반환합니다. "
            + "조회는 읽기 전용이며 side effect 를 발생시키지 않습니다."
    )
    @GetMapping
    public ResponseEntity<List<PurchaseRequestResponse>> getPurchaseRequests(
        @RequestParam(required = false) RequestStatus status,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/purchase-requests - 구입 요청 목록 조회 (status={})", status);
        List<PurchaseRequestResponse> response = purchaseRequestService.getPurchaseRequests(
            userDetails.getUserId(), userDetails.isAdminOrManager(), status
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "구입 요청 상세 조회",
        description = "구입 요청 단건 상세 정보를 조회합니다. ADMIN 과 MANAGER 는 모든 요청을 조회할 수 있고, "
            + "일반 사용자는 본인 요청만 조회할 수 있습니다. "
            + "응답에는 대상 과목, 요청자, 금액, 요청 상태, 승인자, 승인 시각, 반려 메모가 포함될 수 있으며 조회 자체는 side effect 를 발생시키지 않습니다."
    )
    @GetMapping("/{requestId}")
    public ResponseEntity<PurchaseRequestResponse> getPurchaseRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/purchase-requests/{} - 구입 요청 상세 조회", requestId);
        PurchaseRequestResponse response = purchaseRequestService.getPurchaseRequest(
            userDetails.getUserId(), requestId, userDetails.isAdminOrManager()
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
        summary = "구입 요청 승인",
        description = "ADMIN 또는 MANAGER 가 PENDING 상태의 구입 요청을 승인합니다. "
            + "승인 시 요청 상태는 APPROVED 로 변경되고 승인자 및 승인 시각이 저장됩니다. "
            + "현재 구현에서는 추가 도메인 이벤트나 정산 처리 같은 side effect 는 발생하지 않고 요청 상태만 변경됩니다. "
            + "이미 처리된 요청은 다시 승인할 수 없습니다."
    )
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
    @Operation(
        summary = "구입 요청 반려",
        description = "ADMIN 또는 MANAGER 가 PENDING 상태의 구입 요청을 반려합니다. "
            + "반려 시 요청 상태는 REJECTED 로 변경되고 승인자, 승인 시각, 반려 사유(note)가 저장됩니다. "
            + "반려는 요청 상태만 변경하며 추가적인 side effect 는 발생하지 않습니다. "
            + "이미 처리된 요청은 다시 반려할 수 없습니다."
    )
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
