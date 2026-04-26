package geumjeongyahak.domain.request.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.request.service.LessonExchangeProposalService;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeProposalRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
