package geumjeongyahak.domain.request.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.request.service.LessonExchangeProposalService;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeProposalRequest;
import geumjeongyahak.domain.request.v1.dto.request.UpdateLessonExchangeProposalRequest;
import geumjeongyahak.domain.request.v1.dto.response.LessonExchangeProposalResponse;
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
@Tag(name = "LessonExchangeProposal", description = "수업 교환 제안 API")
public class LessonExchangeProposalController {

    private final LessonExchangeProposalService lessonExchangeProposalService;

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "수업 교환 제안 생성",
        description = "인증된 사용자가 APPROVED 상태의 수업 교환 요청에 대해 교환 제안을 생성합니다. "
            + "lessonDate를 입력하면 교환형 제안으로 처리됩니다. "
            + "lessonDate를 입력하지 않으면 대체형 제안으로 처리됩니다. "
            + "교환형 제안에서 startPeriod/endPeriod를 입력하지 않으면 하루 전체 수업 제안으로 처리됩니다."
    )
    @PostMapping("/{requestId}/proposals")
    public ResponseEntity<LessonExchangeProposalResponse> createLessonExchangeProposal(
        @PathVariable Long requestId,
        @Valid @RequestBody CreateLessonExchangeProposalRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("POST /api/v1/lesson-exchange-requests/{}/proposals - 수업 교환 제안 생성", requestId);
        LessonExchangeProposalResponse response = lessonExchangeProposalService.createLessonExchangeProposal(
            userDetails.getUserId(),
            requestId,
            request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "수업 교환 제안 목록 조회",
        description = "특정 수업 교환 요청에 등록된 제안 목록을 조회합니다. "
            + "교환형 제안과 대체형 제안을 함께 반환하며, 최신 생성 순으로 정렬됩니다."
    )
    @GetMapping("/{requestId}/proposals")
    public ResponseEntity<List<LessonExchangeProposalResponse>> getLessonExchangeProposals(
        @PathVariable Long requestId
    ) {
        log.debug("GET /api/v1/lesson-exchange-requests/{}/proposals - 수업 교환 제안 목록 조회", requestId);
        List<LessonExchangeProposalResponse> response = lessonExchangeProposalService.getLessonExchangeProposals(
            requestId
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "수업 교환 제안 수정",
        description = "인증된 사용자가 본인이 작성한 ACTIVE 상태의 수업 교환 제안을 수정합니다. "
            + "입력 규칙은 제안 생성 API와 동일하며, 요청이 여전히 제안 가능 상태인지와 제안자가 실제 수업 조건을 만족하는지도 다시 검증합니다."
    )
    @PatchMapping("/{requestId}/proposals/{proposalId}")
    public ResponseEntity<LessonExchangeProposalResponse> updateLessonExchangeProposal(
        @PathVariable Long requestId,
        @PathVariable Long proposalId,
        @Valid @RequestBody UpdateLessonExchangeProposalRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lesson-exchange-requests/{}/proposals/{} - 수업 교환 제안 수정", requestId, proposalId);
        LessonExchangeProposalResponse response = lessonExchangeProposalService.updateLessonExchangeProposal(
            userDetails.getUserId(),
            requestId,
            proposalId,
            request
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "수업 교환 제안 철회",
        description = "인증된 사용자가 본인이 작성한 ACTIVE 상태의 수업 교환 제안을 철회합니다. "
            + "철회 시 제안 상태는 WITHDRAWN 으로 변경되고 철회 시각이 기록됩니다."
    )
    @PatchMapping("/{requestId}/proposals/{proposalId}/withdraw")
    public ResponseEntity<LessonExchangeProposalResponse> withdrawLessonExchangeProposal(
        @PathVariable Long requestId,
        @PathVariable Long proposalId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lesson-exchange-requests/{}/proposals/{}/withdraw - 수업 교환 제안 철회",
            requestId, proposalId);
        LessonExchangeProposalResponse response = lessonExchangeProposalService.withdrawLessonExchangeProposal(
            userDetails.getUserId(),
            requestId,
            proposalId
        );
        return ResponseEntity.ok(response);
    }
}
