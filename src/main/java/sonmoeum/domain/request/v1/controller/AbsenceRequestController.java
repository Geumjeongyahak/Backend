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
import org.springframework.web.bind.annotation.DeleteMapping;
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
import sonmoeum.domain.request.service.AbsenceRequestService;
import sonmoeum.domain.request.v1.dto.request.CreateAbsenceRequestRequest;
import sonmoeum.domain.request.v1.dto.request.RejectRequestRequest;
import sonmoeum.domain.request.v1.dto.response.AbsenceRequestResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/absence-requests")
@RequiredArgsConstructor
@Tag(name = "AbsenceRequest", description = "결석 요청 API")
public class AbsenceRequestController {

    private final AbsenceRequestService absenceRequestService;

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "결석 요청 생성", description = "결석 요청을 생성합니다.")
    @PostMapping
    public ResponseEntity<AbsenceRequestResponse> createAbsenceRequest(
        @Valid @RequestBody CreateAbsenceRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/absence-requests - 결석 요청 생성 (lessonId={})", request.lessonId());
        AbsenceRequestResponse response = absenceRequestService.createAbsenceRequest(
            userDetails.getUserId(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "결석 요청 목록 조회", description = "결석 요청 목록을 조회합니다. 관리자는 전체, 일반 사용자는 본인 요청만 조회됩니다.")
    @GetMapping
    public ResponseEntity<List<AbsenceRequestResponse>> getAbsenceRequests(
        @RequestParam(required = false) RequestStatus status,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/absence-requests - 결석 요청 목록 조회 (status={})", status);
        List<AbsenceRequestResponse> response = absenceRequestService.getAbsenceRequests(
            userDetails.getUserId(), userDetails.isAdmin(), status
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "결석 요청 상세 조회", description = "결석 요청 상세 정보를 조회합니다.")
    @GetMapping("/{requestId}")
    public ResponseEntity<AbsenceRequestResponse> getAbsenceRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/absence-requests/{} - 결석 요청 상세 조회", requestId);
        AbsenceRequestResponse response = absenceRequestService.getAbsenceRequest(
            userDetails.getUserId(), requestId, userDetails.isAdmin()
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "결석 요청 승인", description = "결석 요청을 승인하고 수업 출석 상태를 공결로 업데이트합니다.")
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<AbsenceRequestResponse> approveAbsenceRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/absence-requests/{}/approve - 결석 요청 승인", requestId);
        AbsenceRequestResponse response = absenceRequestService.approveAbsenceRequest(
            userDetails.getUserId(), requestId
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "결석 요청 반려", description = "결석 요청을 반려합니다.")
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<AbsenceRequestResponse> rejectAbsenceRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody RejectRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/absence-requests/{}/reject - 결석 요청 반려", requestId);
        AbsenceRequestResponse response = absenceRequestService.rejectAbsenceRequest(
            userDetails.getUserId(), requestId, request.note()
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "결석 요청 삭제", description = "결석 요청을 삭제합니다. 관리자 또는 요청자 본인만 삭제 가능합니다.")
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deleteAbsenceRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/absence-requests/{} - 결석 요청 삭제", requestId);
        absenceRequestService.deleteAbsenceRequest(
            userDetails.getUserId(), requestId, userDetails.isAdmin()
        );
        return ResponseEntity.noContent().build();
    }
}
