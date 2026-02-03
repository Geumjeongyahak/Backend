package sonmoeum.api.v1.requests;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.ApiResponse;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.requests.dto.request.CreateAbsenceRequest;
import sonmoeum.api.v1.requests.dto.request.RequestStatusUpdateRequest;
import sonmoeum.api.v1.requests.dto.response.AbsenceRequestResponse;
import sonmoeum.common.security.service.CustomUserDetails;
import sonmoeum.domain.request.service.AbsenceRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Absence Requests", description = "결석 요청 관리 API")
@RestController
@RequestMapping("/api/v1/requests/absence")
@RequiredArgsConstructor
public class AbsenceRequestController {

    private final AbsenceRequestService absenceRequestService;

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_REQUESTS')")
    @Operation(summary = "결석 요청 목록 조회", description = "페이지네이션된 결석 요청 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<BasePageResponse<AbsenceRequestResponse>> getAbsenceRequests(BasePageRequest pageRequest) {
        return ApiResponse.success(absenceRequestService.getAbsenceRequestPagination(pageRequest));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_REQUESTS')")
    @Operation(summary = "결석 요청 상세 조회", description = "ID로 결석 요청을 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<AbsenceRequestResponse> getAbsenceRequest(@PathVariable Long id) {
        return ApiResponse.success(absenceRequestService.getAbsenceRequestById(id));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "결석 요청 생성", description = "새로운 결석 요청을 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AbsenceRequestResponse> createAbsenceRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateAbsenceRequest request) {
        if (userDetails == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return ApiResponse.success(absenceRequestService.createAbsenceRequest(userDetails.getUserId(), request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_REQUESTS')")
    @Operation(summary = "결석 요청 상태 변경", description = "결석 요청을 승인하거나 반려합니다.")
    @PatchMapping("/{id}/status")
    public ApiResponse<AbsenceRequestResponse> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RequestStatusUpdateRequest request) {
        if (userDetails == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return ApiResponse.success(absenceRequestService.updateStatus(id, userDetails.getUserId(), request));
    }
}
