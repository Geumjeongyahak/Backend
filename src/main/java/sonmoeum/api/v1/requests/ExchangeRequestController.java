package sonmoeum.api.v1.requests;

import sonmoeum.api.v1.common.dto.request.BasePageRequest;
import sonmoeum.api.v1.common.dto.response.ApiResponse;
import sonmoeum.api.v1.common.dto.response.BasePageResponse;
import sonmoeum.api.v1.requests.dto.request.CreateExchangeRequest;
import sonmoeum.api.v1.requests.dto.request.RequestStatusUpdateRequest;
import sonmoeum.api.v1.requests.dto.response.ExchangeRequestResponse;
import sonmoeum.common.security.service.CustomUserDetails;
import sonmoeum.domain.request.service.ExchangeRequestService;
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

@Tag(name = "Exchange Requests", description = "수업/과목 교환 요청 관리 API")
@RestController
@RequestMapping("/api/v1/requests/exchange")
@RequiredArgsConstructor
public class ExchangeRequestController {

    private final ExchangeRequestService exchangeRequestService;

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_REQUESTS')")
    @Operation(summary = "수업 교환 요청 목록 조회", description = "페이지네이션된 수업 교환 요청 목록을 조회합니다.")
    @GetMapping("/lessons")
    public ApiResponse<BasePageResponse<ExchangeRequestResponse>> getLessonExchangeRequests(
            BasePageRequest pageRequest) {
        return ApiResponse.success(exchangeRequestService.getLessonExchangePagination(pageRequest));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_REQUESTS')")
    @Operation(summary = "과목 교환 요청 목록 조회", description = "페이지네이션된 과목 교환 요청 목록을 조회합니다.")
    @GetMapping("/subjects")
    public ApiResponse<BasePageResponse<ExchangeRequestResponse>> getSubjectExchangeRequests(
            BasePageRequest pageRequest) {
        return ApiResponse.success(exchangeRequestService.getSubjectExchangePagination(pageRequest));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "수업 교환 요청 생성", description = "새로운 수업 교환 요청을 생성합니다.")
    @PostMapping("/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExchangeRequestResponse> createLessonExchangeRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateExchangeRequest request) {
        if (userDetails == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return ApiResponse.success(exchangeRequestService.createLessonExchange(userDetails.getUserId(), request));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "과목 교환 요청 생성", description = "새로운 과목 교환 요청을 생성합니다.")
    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExchangeRequestResponse> createSubjectExchangeRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateExchangeRequest request) {
        if (userDetails == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return ApiResponse.success(exchangeRequestService.createSubjectExchange(userDetails.getUserId(), request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_REQUESTS')")
    @Operation(summary = "수업 교환 요청 상태 변경", description = "수업 교환 요청을 승인하거나 반려합니다.")
    @PatchMapping("/lessons/{id}/status")
    public ApiResponse<ExchangeRequestResponse> updateLessonExchangeStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RequestStatusUpdateRequest request) {
        if (userDetails == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return ApiResponse
                .success(exchangeRequestService.updateLessonExchangeStatus(id, userDetails.getUserId(), request));
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN') or hasAuthority('MANAGE_REQUESTS')")
    @Operation(summary = "과목 교환 요청 상태 변경", description = "과목 교환 요청을 승인하거나 반려합니다.")
    @PatchMapping("/subjects/{id}/status")
    public ApiResponse<ExchangeRequestResponse> updateSubjectExchangeStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RequestStatusUpdateRequest request) {
        if (userDetails == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return ApiResponse
                .success(exchangeRequestService.updateSubjectExchangeStatus(id, userDetails.getUserId(), request));
    }
}
