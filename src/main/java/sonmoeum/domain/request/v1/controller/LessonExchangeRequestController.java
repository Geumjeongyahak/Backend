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
import sonmoeum.domain.request.service.LessonExchangeRequestService;
import sonmoeum.domain.request.v1.dto.request.ApproveLessonExchangeRequest;
import sonmoeum.domain.request.v1.dto.request.CreateLessonExchangeRequestRequest;
import sonmoeum.domain.request.v1.dto.request.RejectRequestRequest;
import sonmoeum.domain.request.v1.dto.response.LessonExchangeRequestResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/lesson-exchange-requests")
@RequiredArgsConstructor
@Tag(name = "LessonExchangeRequest", description = "수업 교환 요청 API")
public class LessonExchangeRequestController {

    private final LessonExchangeRequestService lessonExchangeRequestService;

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "수업 교환 요청 생성", description = "수업 교환 요청을 생성합니다.")
    @PostMapping
    public ResponseEntity<LessonExchangeRequestResponse> createLessonExchangeRequest(
        @Valid @RequestBody CreateLessonExchangeRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/lesson-exchange-requests - 수업 교환 요청 생성 (lessonId={})", request.lessonId());
        LessonExchangeRequestResponse response = lessonExchangeRequestService.createLessonExchangeRequest(
            userDetails.getUserId(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "수업 교환 요청 목록 조회", description = "수업 교환 요청 목록을 조회합니다. 관리자는 전체, 일반 사용자는 본인 요청만 조회됩니다.")
    @GetMapping
    public ResponseEntity<List<LessonExchangeRequestResponse>> getLessonExchangeRequests(
        @RequestParam(required = false) RequestStatus status,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lesson-exchange-requests - 수업 교환 요청 목록 조회 (status={})", status);
        List<LessonExchangeRequestResponse> response = lessonExchangeRequestService.getLessonExchangeRequests(
            userDetails.getUserId(), userDetails.isAdmin(), status
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "수업 교환 요청 상세 조회", description = "수업 교환 요청 상세 정보를 조회합니다.")
    @GetMapping("/{requestId}")
    public ResponseEntity<LessonExchangeRequestResponse> getLessonExchangeRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lesson-exchange-requests/{} - 수업 교환 요청 상세 조회", requestId);
        LessonExchangeRequestResponse response = lessonExchangeRequestService.getLessonExchangeRequest(
            userDetails.getUserId(), requestId, userDetails.isAdmin()
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "수업 교환 요청 승인", description = "수업 교환 요청을 승인하고 수업 담당 교사를 변경합니다.")
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<LessonExchangeRequestResponse> approveLessonExchangeRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody ApproveLessonExchangeRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lesson-exchange-requests/{}/approve - 수업 교환 요청 승인", requestId);
        LessonExchangeRequestResponse response = lessonExchangeRequestService.approveLessonExchangeRequest(
            userDetails.getUserId(), requestId, request.exchangeWithUserId()
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "수업 교환 요청 반려", description = "수업 교환 요청을 반려합니다.")
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<LessonExchangeRequestResponse> rejectLessonExchangeRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody RejectRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lesson-exchange-requests/{}/reject - 수업 교환 요청 반려", requestId);
        LessonExchangeRequestResponse response = lessonExchangeRequestService.rejectLessonExchangeRequest(
            userDetails.getUserId(), requestId, request.note()
        );
        return ResponseEntity.ok(response);
    }
}
