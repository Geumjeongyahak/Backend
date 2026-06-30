package geumjeongyahak.domain.request.service;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.request.v1.dto.request.AbsenceRequestPaginationRequest;
import geumjeongyahak.domain.request.v1.dto.response.AbsenceRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbsenceRequestAdminViewService {

    private static final int FIRST_PAGE = 0;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final AbsenceRequestService absenceRequestService;

    public AdminPage<AbsenceRequestResponse> getAbsenceRequests(Long requesterId, AbsenceRequestFilter filter) {
        AbsenceRequestPaginationRequest pageRequest = new AbsenceRequestPaginationRequest();
        pageRequest.setKeyword(filter.keyword());
        pageRequest.setPage(filter.page() == null || filter.page() < FIRST_PAGE ? FIRST_PAGE : filter.page());
        pageRequest.setSize(filter.size() == null || filter.size() < MIN_PAGE_SIZE
            ? DEFAULT_PAGE_SIZE
            : Math.min(filter.size(), MAX_PAGE_SIZE));

        PaginationResponse<AbsenceRequestResponse> response = absenceRequestService.getAbsenceRequests(
            requesterId,
            filter.status(),
            pageRequest
        );

        return AdminPage.from(response);
    }

    public RequestStatus[] getStatuses() {
        return RequestStatus.values();
    }

    public String getStatusLabel(RequestStatus status) {
        return switch (status) {
            case PENDING -> "승인 대기";
            case APPROVED -> "승인";
            case REJECTED -> "반려";
            case CANCELLED -> "취소";
            case EXPIRED -> "만료";
        };
    }

    @Transactional
    public void approve(Long approverId, Long requestId) {
        absenceRequestService.approveAbsenceRequest(approverId, requestId);
    }

    @Transactional
    public void reject(Long approverId, Long requestId, String note) {
        absenceRequestService.rejectAbsenceRequest(approverId, requestId, note);
    }

    public record AbsenceRequestFilter(
        RequestStatus status,
        String keyword,
        Integer page,
        Integer size
    ) {
    }
}
