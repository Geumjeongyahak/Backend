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
    @Operation(
        summary = "수업 교환 요청 생성",
        description = "인증된 사용자가 자신이 담당 중인 수업에 대해서만 수업 교환 요청을 생성합니다. "
            + "요청 생성 시 상태는 PENDING 으로 저장되며 대상 수업, 요청자, 제목, 본문이 함께 기록됩니다. "
            + "이 단계에서는 실제 담당 교사 변경이 일어나지 않으며, side effect 는 승인 API에서만 발생합니다."
    )
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
    @Operation(
        summary = "수업 교환 요청 목록 조회",
        description = "수업 교환 요청 목록을 조회합니다. ADMIN 과 MANAGER 는 전체 요청을 조회할 수 있고, "
            + "일반 사용자는 본인이 생성한 요청만 조회할 수 있습니다. "
            + "status 파라미터를 전달하면 해당 상태의 요청만 반환합니다. "
            + "조회 API는 side effect 를 발생시키지 않습니다."
    )
    @GetMapping
    public ResponseEntity<List<LessonExchangeRequestResponse>> getLessonExchangeRequests(
        @RequestParam(required = false) RequestStatus status,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lesson-exchange-requests - 수업 교환 요청 목록 조회 (status={})", status);
        List<LessonExchangeRequestResponse> response = lessonExchangeRequestService.getLessonExchangeRequests(
            userDetails.getUserId(), userDetails.isAdminOrManager(), status
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "수업 교환 요청 상세 조회",
        description = "수업 교환 요청 단건을 조회합니다. ADMIN 과 MANAGER 는 모든 요청을 조회할 수 있고, "
            + "일반 사용자는 본인 요청만 조회할 수 있습니다. "
            + "응답에는 대상 수업, 요청자, 제목/내용, 요청 상태, 승인자, 승인 시각, 반려 메모가 포함될 수 있으며 조회 자체는 side effect 를 발생시키지 않습니다."
    )
    @GetMapping("/{requestId}")
    public ResponseEntity<LessonExchangeRequestResponse> getLessonExchangeRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lesson-exchange-requests/{} - 수업 교환 요청 상세 조회", requestId);
        LessonExchangeRequestResponse response = lessonExchangeRequestService.getLessonExchangeRequest(
            userDetails.getUserId(), requestId, userDetails.isAdminOrManager()
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
        summary = "수업 교환 요청 승인",
        description = "ADMIN 또는 MANAGER 가 PENDING 상태의 수업 교환 요청을 승인합니다. "
            + "exchangeWithUserId 로 교환 대상 교사를 지정해야 하며, 요청 상태는 APPROVED 로 변경되고 승인자 및 승인 시각이 기록됩니다. "
            + "승인 이벤트가 발행되면 대상 수업의 담당 교사가 exchangeWithUserId 사용자로 변경됩니다. "
            + "또한 동일한 날짜와 시간대에 교환 대상 교사가 맡고 있는 다른 수업이 존재하면, 해당 수업의 담당 교사는 요청자로 교체되어 "
            + "양방향 스왑 side effect 가 함께 적용됩니다. 이미 처리된 요청은 다시 승인할 수 없습니다."
    )
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
    @Operation(
        summary = "수업 교환 요청 반려",
        description = "ADMIN 또는 MANAGER 가 PENDING 상태의 수업 교환 요청을 반려합니다. "
            + "반려 시 요청 상태는 REJECTED 로 변경되고 승인자, 승인 시각, 반려 사유(note)가 저장됩니다. "
            + "반려는 요청 상태만 변경하며 실제 수업 담당 교사 변경 side effect 는 발생하지 않습니다. "
            + "이미 처리된 요청은 다시 반려할 수 없습니다."
    )
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
