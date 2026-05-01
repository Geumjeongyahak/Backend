package geumjeongyahak.domain.request.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.service.LessonExchangeRequestService;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeRequestRequest;
import geumjeongyahak.domain.request.v1.dto.request.RejectRequestRequest;
import geumjeongyahak.domain.request.v1.dto.request.UpdateLessonExchangeRequestRequest;
import geumjeongyahak.domain.request.v1.dto.response.LessonExchangeRequestDetailResponse;
import geumjeongyahak.domain.request.v1.dto.response.LessonExchangeRequestSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/lesson-exchange-requests")
@RequiredArgsConstructor
@Tag(name = "LessonExchangeRequest", description = "수업 교환 요청 API")
public class LessonExchangeRequestController {

    private static final String TEACHER_OR_HIGHER_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN')";
    private static final String MANAGER_OR_HIGHER_ACCESS =
        "hasRole('MANAGER') or hasRole('ADMIN')";

    private final LessonExchangeRequestService lessonExchangeRequestService;

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 교환 요청 생성",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 자신이 담당 중인 수업에 대해서만 수업 교환 요청을 생성합니다. "
            + "요청 생성 시 PENDING 상태(승인 대기 상태)로 저장되며 대상 수업, 요청자, 제목, 내용, 교환 범위, 교시 범위, 만료 시각이 함께 저장됩니다. "
            + "반 이름은 생성 시점의 표시값을 snapshot 으로 함께 저장하며, 이후 실제 수업 교사가 변경되더라도 요청 화면에는 기존 값이 유지됩니다. "
            + "이 단계에서는 실제 담당 교사 변경이 일어나지 않으며, side effect(승인/제안/수락)는 승인 API에서만 발생합니다. "
            + "만료 시각이 지나면 요청은 스케줄러에 의해 EXPIRED 상태로 자동 전환되며, 남아 있는 ACTIVE 제안도 함께 정리됩니다. "
            + "수업 교환 요청은 현재 기준 4일 이후 수업부터 가능합니다. (현재 6월 1일인 경우, 6월 5일 수업부터 교환 신청 가능) "
            + "수업 교환 요청의 만료 시각은 교환 대상 수업의 3일 전 23:59:59까지 설정 가능합니다. (6월 5일 수업인 경우 6월 2일 23:59:59까지 설정 가능)"
    )
    @PostMapping
    public ResponseEntity<LessonExchangeRequestDetailResponse> createLessonExchangeRequest(
        @Valid @RequestBody CreateLessonExchangeRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/lesson-exchange-requests - 수업 교환 요청 생성 (title={})", request.title());
        LessonExchangeRequestDetailResponse response = lessonExchangeRequestService.createLessonExchangeRequest(
            userDetails.getUserId(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 교환 요청 목록 조회",
        description = "수업 교환 요청 목록을 조회합니다. 모든 교원은 모든 요청을 조회할 수 있습니다. "
            + "mine 파라미터를 전달하면 본인이 요청한 요청만 반환합니다. "
            + "status 파라미터를 전달하면 해당 상태의 요청만 반환합니다. "
            + "status 파라미터가 없으면 CANCELLED 상태 요청은 기본 목록에서 제외됩니다. "
            + "응답의 반 이름은 현재 수업을 다시 조회하지 않고 생성/수정 당시 저장한 snapshot 값을 사용합니다. "
            + "따라서 제안 수락 이후 실제 lesson 의 담당 교사가 변경되더라도 요청 화면에 보이는 반 이름은 기존 값이 유지됩니다. "
            + "조회 API는 side effect 를 발생시키지 않습니다."
    )
    @GetMapping
    public ResponseEntity<List<LessonExchangeRequestSummaryResponse>> getLessonExchangeRequests(
        @RequestParam(required = false) LessonExchangeRequestStatus status,
        @RequestParam(defaultValue = "false") boolean mine,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/lesson-exchange-requests - 수업 교환 요청 목록 조회 (status={}, mine={})", status, mine);
        List<LessonExchangeRequestSummaryResponse> response = lessonExchangeRequestService.getLessonExchangeRequests(
            userDetails.getUserId(), status, mine
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 교환 요청 상세 조회",
        description = "수업 교환 요청 단건을 조회합니다. 모든 교원은 모든 요청을 조회할 수 있습니다. "
            + "응답에는 대상 수업, 반 이름, 요청자, 제목/내용, 요청 상태, 교환 범위, 교시 범위, 만료 시각, 처리 정보가 포함됩니다. "
            + "반 이름은 생성/수정 당시 저장한 snapshot 값을 사용하므로, 제안 수락 이후 실제 수업 교사가 변경되더라도 화면 표시값은 유지됩니다. "
            + "조회 API는 side effect 를 발생시키지 않습니다."
    )
    @GetMapping("/{requestId}")
    public ResponseEntity<LessonExchangeRequestDetailResponse> getLessonExchangeRequest(
        @PathVariable Long requestId
    ) {
        log.debug("GET /api/v1/lesson-exchange-requests/{} - 수업 교환 요청 상세 조회", requestId);
        LessonExchangeRequestDetailResponse response = lessonExchangeRequestService.getLessonExchangeRequest(
            requestId
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 교환 요청 수정",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 본인이 생성한 PENDING 상태의 수업 교환 요청을 수정합니다. "
            + "생성 API와 동일한 입력 구조를 사용하며, 수정 후에도 대상 수업, 교환 범위, 만료 시각 정책 검증을 동일하게 수행합니다. "
            + "반 이름 snapshot 도 함께 갱신되며, 이후 조회 시에는 최신 수정 기준의 표시값이 유지됩니다. "
            + "승인 이후 상태의 요청은 수정할 수 없으며, 실제 수업 교환 side effect 는 발생하지 않습니다."
    )
    @PatchMapping("/{requestId}")
    public ResponseEntity<LessonExchangeRequestDetailResponse> updateLessonExchangeRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody UpdateLessonExchangeRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lesson-exchange-requests/{} - 수업 교환 요청 수정", requestId);
        LessonExchangeRequestDetailResponse response = lessonExchangeRequestService.updateLessonExchangeRequest(
            userDetails.getUserId(), requestId, request
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 교환 요청 취소",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 본인이 생성한 PENDING 상태의 수업 교환 요청을 취소합니다. "
            + "취소 시 요청 상태는 CANCELLED 로 변경되고 취소 시각이 기록됩니다. "
            + "승인 이후 상태의 요청은 취소할 수 없으며, 실제 수업 교환 side effect 는 발생하지 않습니다."
    )
    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<LessonExchangeRequestDetailResponse> cancelLessonExchangeRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lesson-exchange-requests/{}/cancel - 수업 교환 요청 취소", requestId);
        LessonExchangeRequestDetailResponse response = lessonExchangeRequestService.cancelLessonExchangeRequest(
            userDetails.getUserId(), requestId
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(MANAGER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 교환 요청 승인",
        description = "ADMIN 또는 MANAGER 가 PENDING 상태의 수업 교환 요청을 승인합니다. "
            + "요청 상태는 APPROVED 로 변경되고 처리자 및 처리 시각이 기록됩니다. "
            + "이 단계에서는 실제 수업 교환 side effect 는 발생하지 않으며, "
            + "이후 다른 교원이 해당 요청에 대해 교환 제안을 작성할 수 있게 됩니다."
    )
    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<LessonExchangeRequestDetailResponse> approveLessonExchangeRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lesson-exchange-requests/{}/approve - 수업 교환 요청 승인", requestId);
        LessonExchangeRequestDetailResponse response = lessonExchangeRequestService.approveLessonExchangeRequest(
            userDetails.getUserId(), requestId
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(MANAGER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 교환 요청 반려",
        description = "ADMIN 또는 MANAGER 가 PENDING 상태의 수업 교환 요청을 반려합니다. "
            + "반려 시 요청 상태는 REJECTED 로 변경되고 처리자, 처리 시각, 반려 사유(note)가 저장됩니다. "
            + "이 단계에서는 실제 수업 교환 side effect 는 발생하지 않으며, "
            + "이미 처리된 요청은 다시 반려할 수 없습니다."
    )
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<LessonExchangeRequestDetailResponse> rejectLessonExchangeRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody RejectRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lesson-exchange-requests/{}/reject - 수업 교환 요청 반려", requestId);
        LessonExchangeRequestDetailResponse response = lessonExchangeRequestService.rejectLessonExchangeRequest(
            userDetails.getUserId(), requestId, request.note()
        );
        return ResponseEntity.ok(response);
    }
}
