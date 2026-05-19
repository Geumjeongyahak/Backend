package geumjeongyahak.domain.request.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.request.service.AbsenceRequestService;
import geumjeongyahak.domain.request.v1.dto.request.AbsenceRequestPaginationRequest;
import geumjeongyahak.domain.request.v1.dto.request.CreateAbsenceRequestRequest;
import geumjeongyahak.domain.request.v1.dto.request.RejectRequestRequest;
import geumjeongyahak.domain.request.v1.dto.request.UpdateAbsenceRequestRequest;
import geumjeongyahak.domain.request.v1.dto.response.AbsenceRequestResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/absence-requests")
@RequiredArgsConstructor
@Tag(name = "AbsenceRequest", description = "결석 요청 API")
public class AbsenceRequestController {

    private static final String TEACHER_OR_HIGHER_ACCESS =
        "hasRole('VOLUNTEER') or hasRole('MANAGER') or hasRole('ADMIN')";
    private static final String ABSENCE_REQUEST_READ_ACCESS =
        TEACHER_OR_HIGHER_ACCESS + " or hasAuthority('absence-request:read:*')";
    private static final String ABSENCE_REQUEST_MANAGE_ACCESS =
        "hasRole('ADMIN') or hasAuthority('absence-request:manage:*')";

    private final AbsenceRequestService absenceRequestService;

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "결석 요청 생성",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 사용자가 자신이 담당 중인 하루 일정에 대해서만 결석 요청을 생성합니다. "
            + "ADMIN 또는 MANAGER 권한이 있더라도 대상 하루 일정의 담당 교사가 아니면 대리 생성할 수 없습니다. "
            + "요청 생성 시 PENDING 상태(승인 대기 상태)로 저장되며 대상 하루 일정, 요청자, 결석 사유, 만료 시각이 함께 저장됩니다. "
            + "만료 시각(expiresAt)은 요청 body로 받지 않고 대상 하루 일정 수업일의 00:00으로 자동 설정됩니다. "
            + "즉 수업 전날까지 승인/반려 처리가 완료되어야 하며, 수업일 00:00이 지나면 PENDING 요청은 스케줄러에 의해 EXPIRED 상태로 자동 전환됩니다. "
            + "같은 하루 일정과 같은 요청자 기준으로 PENDING 또는 APPROVED 결석 요청이 이미 있으면 중복 생성할 수 없습니다. "
            + "REJECTED 또는 CANCELLED 요청은 재요청을 막지 않습니다. "
            + "이 단계에서는 DailySchedule 교사 출석 상태를 변경하지 않으며, 실제 출석 side effect 는 승인 API에서만 발생합니다."
    )
    @PostMapping
    public ResponseEntity<AbsenceRequestResponse> createAbsenceRequest(
        @Valid @RequestBody CreateAbsenceRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug(
            "POST /api/v1/absence-requests - 결석 요청 생성 (lessonDate={})",
            request.lessonDate()
        );
        AbsenceRequestResponse response = absenceRequestService.createAbsenceRequest(
            userDetails.getUserId(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize(ABSENCE_REQUEST_READ_ACCESS)
    @Operation(
        summary = "결석 요청 목록 조회",
        description = "VOLUNTEER, MANAGER, ADMIN 또는 absence-request:read:* 권한 사용자가 결석 요청 목록을 페이지로 조회합니다. "
            + "ADMIN 또는 absence-request:read:* 권한 사용자는 전체 결석 요청을 조회할 수 있습니다. "
            + "그 외 VOLUNTEER, MANAGER 사용자는 본인이 생성한 요청만 조회할 수 있으며, MANAGER 권한만으로는 전체 요청을 조회할 수 없습니다. "
            + "status 파라미터를 전달하면 해당 상태의 요청만 반환합니다. "
            + "keyword 파라미터로 제목, 사유, 작성자 이름, 반 이름을 검색할 수 있습니다. "
            + "page, size 파라미터를 통해 페이지 번호와 크기를 지정할 수 있고 기본 정렬은 생성 시각 최신순입니다. "
            + "응답에는 대상 하루 일정 수업일 기준으로 자동 계산된 만료 시각(expiresAt)이 포함됩니다. "
            + "조회 API는 side effect 를 발생시키지 않습니다."
    )
    @GetMapping
    public ResponseEntity<PaginationResponse<AbsenceRequestResponse>> getAbsenceRequests(
        @RequestParam(required = false) RequestStatus status,
        @ParameterObject @Valid @ModelAttribute AbsenceRequestPaginationRequest pageRequest,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/absence-requests - 결석 요청 목록 조회 (status={})", status);
        PaginationResponse<AbsenceRequestResponse> response = absenceRequestService.getAbsenceRequests(
            userDetails.getUserId(), canReadAllAbsenceRequests(userDetails), status, pageRequest
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(ABSENCE_REQUEST_READ_ACCESS)
    @Operation(
        summary = "결석 요청 상세 조회",
        description = "VOLUNTEER, MANAGER, ADMIN 또는 absence-request:read:* 권한 사용자가 결석 요청 단건 상세 정보를 조회합니다. "
            + "ADMIN 또는 absence-request:read:* 권한 사용자는 모든 결석 요청을 조회할 수 있습니다. "
            + "그 외 VOLUNTEER, MANAGER 사용자는 본인이 생성한 요청만 조회할 수 있습니다. "
            + "응답에는 대상 하루 일정, 요청자, 결석 사유, 만료 시각, 요청 상태, 승인/반려 정보가 포함됩니다. "
            + "만료 시각은 대상 하루 일정 수업일의 00:00이며, 해당 시각이 지난 PENDING 요청은 EXPIRED 상태로 자동 전환됩니다. "
            + "조회 API는 side effect 를 발생시키지 않습니다."
    )
    @GetMapping("/{requestId}")
    public ResponseEntity<AbsenceRequestResponse> getAbsenceRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/absence-requests/{} - 결석 요청 상세 조회", requestId);
        AbsenceRequestResponse response = absenceRequestService.getAbsenceRequest(
            userDetails.getUserId(), requestId, canReadAllAbsenceRequests(userDetails)
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "결석 요청 수정",
        description = "요청자 본인이 PENDING 상태의 결석 요청 제목과 사유를 수정합니다. "
            + "대리 수정은 허용하지 않으며, 이미 처리된 요청은 수정할 수 없습니다."
    )
    @PatchMapping("/{requestId}")
    public ResponseEntity<AbsenceRequestResponse> updateAbsenceRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody UpdateAbsenceRequestRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("PATCH /api/v1/absence-requests/{} - 결석 요청 수정", requestId);
        AbsenceRequestResponse response = absenceRequestService.updateAbsenceRequest(
            userDetails.getUserId(), requestId, request
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(ABSENCE_REQUEST_MANAGE_ACCESS)
    @Operation(
        summary = "결석 요청 승인",
        description = "ADMIN 또는 absence-request:manage:* 권한 사용자가 PENDING 상태의 결석 요청을 승인합니다. "
            + "승인 시 요청 상태는 APPROVED 로 변경되고 승인자 및 승인 시각이 기록됩니다. "
            + "승인 이벤트를 통해 연결된 DailySchedule 교사 출석은 EXCUSED 로 반영됩니다. "
            + "만료 시각이 지나 EXPIRED 처리된 요청은 승인할 수 없습니다. "
            + "APPROVED, REJECTED, CANCELLED, EXPIRED 상태의 요청은 다시 승인할 수 없습니다."
    )
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

    @PreAuthorize(ABSENCE_REQUEST_MANAGE_ACCESS)
    @Operation(
        summary = "결석 요청 반려",
        description = "ADMIN 또는 absence-request:manage:* 권한 사용자가 PENDING 상태의 결석 요청을 반려합니다. "
            + "반려 시 요청 상태는 REJECTED 로 변경되고 처리자, 처리 시각, 반려 사유(note)가 함께 저장됩니다. "
            + "반려는 요청 상태만 변경하며 DailySchedule 교사 출석 상태를 변경하는 side effect 는 발생하지 않습니다. "
            + "만료 시각이 지나 EXPIRED 처리된 요청은 반려할 수 없습니다. "
            + "APPROVED, REJECTED, CANCELLED, EXPIRED 상태의 요청은 다시 반려할 수 없습니다."
    )
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

    @PreAuthorize(TEACHER_OR_HIGHER_ACCESS)
    @Operation(
        summary = "결석 요청 취소",
        description = "교사 이상 권한(VOLUNTEER, MANAGER, ADMIN)을 가진 요청자 본인이 PENDING 상태의 결석 요청을 취소합니다. "
            + "대리 취소는 허용하지 않으며, ADMIN 또는 MANAGER 권한이 있어도 본인이 생성한 요청이 아니면 취소할 수 없습니다. "
            + "취소 시 요청을 물리 삭제하지 않고 상태를 CANCELLED 로 변경합니다. "
            + "만료 시각이 지나 EXPIRED 처리된 요청은 취소할 수 없습니다. "
            + "APPROVED, REJECTED, CANCELLED, EXPIRED 상태의 요청은 취소할 수 없습니다. "
            + "취소는 DailySchedule 교사 출석 상태를 변경하지 않습니다."
    )
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deleteAbsenceRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/absence-requests/{} - 결석 요청 취소", requestId);
        absenceRequestService.deleteAbsenceRequest(
            userDetails.getUserId(), requestId
        );
        return ResponseEntity.noContent().build();
    }

    private boolean canReadAllAbsenceRequests(CustomUserDetails userDetails) {
        return userDetails.isAdmin()
            || userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("absence-request:read:*"::equals);
    }
}
