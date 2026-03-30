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
import sonmoeum.domain.request.service.SubjectExchangeRequestService;
import sonmoeum.domain.request.v1.dto.request.ApproveSubjectExchangeRequest;
import sonmoeum.domain.request.v1.dto.request.CreateSubjectExchangeRequestRequest;
import sonmoeum.domain.request.v1.dto.request.RejectRequestRequest;
import sonmoeum.domain.request.v1.dto.response.SubjectExchangeRequestResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/subject-exchange-requests")
@RequiredArgsConstructor
@Tag(name = "SubjectExchangeRequest", description = "과목 교환 요청 API")
public class SubjectExchangeRequestController {

    private final SubjectExchangeRequestService subjectExchangeRequestService;

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "과목 교환 요청 생성", description = "과목 교환 요청을 생성합니다.")
    @PostMapping
    public ResponseEntity<SubjectExchangeRequestResponse> createSubjectExchangeRequest(
        @Valid @RequestBody CreateSubjectExchangeRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/subject-exchange-requests - 과목 교환 요청 생성 (subjectId={})", request.subjectId());
        SubjectExchangeRequestResponse response = subjectExchangeRequestService.createSubjectExchangeRequest(
            userDetails.getUserId(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "과목 교환 요청 목록 조회", description = "과목 교환 요청 목록을 조회합니다. 관리자는 전체, 일반 사용자는 본인 요청만 조회됩니다.")
    @GetMapping
    public ResponseEntity<List<SubjectExchangeRequestResponse>> getSubjectExchangeRequests(
        @RequestParam(required = false) RequestStatus status,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/subject-exchange-requests - 과목 교환 요청 목록 조회 (status={})", status);
        List<SubjectExchangeRequestResponse> response = subjectExchangeRequestService.getSubjectExchangeRequests(
            userDetails.getUserId(), userDetails.isAdmin(), status
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "과목 교환 요청 상세 조회", description = "과목 교환 요청 상세 정보를 조회합니다.")
    @GetMapping("/{requestId}")
    public ResponseEntity<SubjectExchangeRequestResponse> getSubjectExchangeRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/subject-exchange-requests/{} - 과목 교환 요청 상세 조회", requestId);
        SubjectExchangeRequestResponse response = subjectExchangeRequestService.getSubjectExchangeRequest(
            userDetails.getUserId(), requestId, userDetails.isAdmin()
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "과목 교환 요청 승인", description = "과목 교환 요청을 승인합니다.")
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<SubjectExchangeRequestResponse> approveSubjectExchangeRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody ApproveSubjectExchangeRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/subject-exchange-requests/{}/approve - 과목 교환 요청 승인", requestId);
        SubjectExchangeRequestResponse response = subjectExchangeRequestService.approveSubjectExchangeRequest(
            userDetails.getUserId(), requestId, request.exchangeWithUserId()
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "과목 교환 요청 반려", description = "과목 교환 요청을 반려합니다.")
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<SubjectExchangeRequestResponse> rejectSubjectExchangeRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody RejectRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/subject-exchange-requests/{}/reject - 과목 교환 요청 반려", requestId);
        SubjectExchangeRequestResponse response = subjectExchangeRequestService.rejectSubjectExchangeRequest(
            userDetails.getUserId(), requestId, request.note()
        );
        return ResponseEntity.ok(response);
    }
}
