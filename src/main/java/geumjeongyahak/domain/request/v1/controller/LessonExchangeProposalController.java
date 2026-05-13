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

    private static final String TEACHER_OR_HIGHER_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN')";

    private final LessonExchangeProposalService lessonExchangeProposalService;

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 교환 제안 생성",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 APPROVED 상태의 수업 교환 요청에 대해 교환 제안을 생성합니다. "
            + "lessonDate를 입력하면 교환형 제안으로 처리됩니다. "
            + "lessonDate를 입력하지 않으면 대체형 제안으로 처리됩니다. "
            + "교환형 제안의 반 이름은 생성 시점의 표시값을 snapshot 으로 함께 저장하며, 이후 실제 수업 교사가 변경되더라도 제안 화면에는 기존 값이 유지됩니다. "
            + "대체형 제안은 제안 자체에 대응하는 교환 수업이 없으므로 반 이름을 별도로 저장하거나 반환하지 않습니다."
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

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 교환 제안 목록 조회",
        description = "특정 수업 교환 요청에 등록된 제안 목록을 조회합니다. "
            + "교환형 제안과 대체형 제안을 함께 반환하며, 최신 생성 순으로 정렬됩니다. "
            + "WITHDRAWN 상태 제안은 기본 목록에서 제외되며, 반 이름은 생성/수정 당시 저장한 snapshot 값을 사용합니다. "
            + "대체형 제안의 반 이름은 null 로 반환됩니다. "
            + "따라서 제안 수락 이후 실제 lesson 의 담당 교사가 변경되더라도 제안 화면에 보이는 반 이름은 기존 값이 유지됩니다."
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

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 교환 제안 수정",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 본인이 작성한 ACTIVE 상태의 수업 교환 제안을 수정합니다. "
            + "입력 규칙은 제안 생성 API와 동일하며, 요청이 여전히 제안 가능 상태인지와 제안자가 실제 수업 조건을 만족하는지도 다시 검증합니다. "
            + "교환형 제안의 반 이름 snapshot 도 함께 갱신되며, 대체형 제안은 반 이름 없이 유지됩니다. "
            + "이후 조회 시에는 최신 수정 기준의 표시값이 유지됩니다."
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

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 교환 제안 수락",
        description = "요청자가 APPROVED 상태의 수업 교환 요청에 등록된 ACTIVE 제안 하나를 수락합니다. "
            + "수락 시 요청은 COMPLETED, 선택된 제안은 ACCEPTED, 나머지 ACTIVE 제안은 CLOSED 상태로 변경되며 실제 수업 교환/대체가 반영됩니다. "
            + "이때 실제 lesson 의 담당 교사는 변경되지만, 요청/제안 응답에 보이는 반 이름은 생성/수정 당시 저장한 snapshot 값을 그대로 유지합니다. "
            + "즉 수락 이후 화면에서는 상태만 변경되고 기존 요청/제안의 표시 내용은 유지됩니다."
    )
    @PatchMapping("/{requestId}/proposals/{proposalId}/accept")
    public ResponseEntity<LessonExchangeProposalResponse> acceptLessonExchangeProposal(
        @PathVariable Long requestId,
        @PathVariable Long proposalId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/lesson-exchange-requests/{}/proposals/{}/accept - 수업 교환 제안 수락",
            requestId, proposalId);
        LessonExchangeProposalResponse response = lessonExchangeProposalService.acceptLessonExchangeProposal(
            userDetails.getUserId(),
            requestId,
            proposalId
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "수업 교환 제안 철회",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 본인이 작성한 ACTIVE 상태의 수업 교환 제안을 철회합니다. "
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
