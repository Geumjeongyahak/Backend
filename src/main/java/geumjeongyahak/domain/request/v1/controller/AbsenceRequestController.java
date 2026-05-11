package geumjeongyahak.domain.request.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
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
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.request.service.AbsenceRequestService;
import geumjeongyahak.domain.request.v1.dto.request.CreateAbsenceRequestRequest;
import geumjeongyahak.domain.request.v1.dto.request.RejectRequestRequest;
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
        description = "VOLUNTEER, MANAGER, ADMIN 권한 사용자가 특정 수업에 대한 결석 요청을 생성합니다. "
            + "요청 생성 시 요청 상태는 PENDING 으로 저장되며, 요청자 정보와 대상 수업 정보가 함께 응답됩니다. "
            + "이 단계에서는 수업 출석 상태를 즉시 변경하지 않으며, 실제 side effect 는 승인 API에서만 발생합니다."
    )
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

    @PreAuthorize(ABSENCE_REQUEST_READ_ACCESS)
    @Operation(
        summary = "결석 요청 목록 조회",
        description = "VOLUNTEER, MANAGER, ADMIN 또는 absence-request:read:* 권한 사용자가 결석 요청 목록을 조회합니다. "
            + "ADMIN 또는 absence-request:read:* 권한 사용자는 전체 요청을 조회할 수 있고, "
            + "그 외 사용자는 본인이 생성한 요청만 조회할 수 있습니다. "
            + "status 쿼리 파라미터가 전달되면 해당 상태로 필터링된 결과만 반환합니다. "
            + "조회 API는 읽기 전용이며 side effect 를 발생시키지 않습니다."
    )
    @GetMapping
    public ResponseEntity<List<AbsenceRequestResponse>> getAbsenceRequests(
        @RequestParam(required = false) RequestStatus status,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("GET /api/v1/absence-requests - 결석 요청 목록 조회 (status={})", status);
        List<AbsenceRequestResponse> response = absenceRequestService.getAbsenceRequests(
            userDetails.getUserId(), canReadAllAbsenceRequests(userDetails), status
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(ABSENCE_REQUEST_READ_ACCESS)
    @Operation(
        summary = "결석 요청 상세 조회",
        description = "VOLUNTEER, MANAGER, ADMIN 또는 absence-request:read:* 권한 사용자가 결석 요청 단건 상세 정보를 조회합니다. "
            + "ADMIN 또는 absence-request:read:* 권한 사용자는 모든 요청을 조회할 수 있고, "
            + "그 외 사용자는 본인 요청만 조회할 수 있습니다. "
            + "응답에는 요청 상태, 결석 사유, 승인자, 승인 시각, 반려 메모가 포함될 수 있으며 조회 자체는 side effect 를 발생시키지 않습니다."
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

    @PreAuthorize(ABSENCE_REQUEST_MANAGE_ACCESS)
    @Operation(
        summary = "결석 요청 승인",
        description = "ADMIN 또는 absence-request:manage:* 권한 사용자가 PENDING 상태의 결석 요청을 승인합니다. "
            + "승인되면 요청 상태가 APPROVED 로 변경되고 승인자 및 승인 시각이 기록됩니다. "
            + "동시에 승인 이벤트가 발행되어 대상 수업의 교사 출석 상태가 EXCUSED(공결)로 변경되는 side effect 가 발생합니다. "
            + "이미 APPROVED 또는 REJECTED 상태인 요청은 다시 승인할 수 없습니다."
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
            + "반려 시 요청 상태는 REJECTED 로 변경되고 승인자, 승인 시각, 반려 사유(note)가 함께 저장됩니다. "
            + "반려는 요청 상태만 변경하며 수업 출석 상태를 변경하는 side effect 는 발생하지 않습니다. "
            + "이미 처리된 요청은 다시 반려할 수 없습니다."
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
        summary = "결석 요청 삭제",
        description = "VOLUNTEER, MANAGER, ADMIN 권한 사용자가 결석 요청을 삭제합니다. "
            + "서비스 정책상 요청자 본인만 삭제할 수 있습니다. "
            + "삭제는 아직 처리되지 않은 PENDING 요청에만 허용되며, 이미 APPROVED 또는 REJECTED 상태인 요청은 "
            + "이력 보존을 위해 삭제할 수 없습니다. "
            + "삭제는 요청 데이터만 제거하며 추가적인 side effect 는 발생하지 않습니다."
    )
    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deleteAbsenceRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("DELETE /api/v1/absence-requests/{} - 결석 요청 삭제", requestId);
        absenceRequestService.deleteAbsenceRequest(
            userDetails.getUserId(), requestId, userDetails.isAdminOrManager()
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
