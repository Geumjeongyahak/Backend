package geumjeongyahak.unit.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.PaginationResponse;
import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.request.service.AbsenceRequestAdminViewService;
import geumjeongyahak.domain.request.service.AbsenceRequestAdminViewService.AbsenceRequestFilter;
import geumjeongyahak.domain.request.service.AbsenceRequestService;
import geumjeongyahak.domain.request.v1.dto.request.AbsenceRequestPaginationRequest;
import geumjeongyahak.domain.request.v1.dto.response.AbsenceRequestResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AbsenceRequestAdminViewServiceTest {

    @Mock
    private AbsenceRequestService absenceRequestService;

    @InjectMocks
    private AbsenceRequestAdminViewService absenceRequestAdminViewService;

    @Test
    void getAbsenceRequests_returnsAdminPageFromPagedServiceResponse() {
        AbsenceRequestResponse response = absenceRequestResponse(10L, RequestStatus.PENDING);
        given(absenceRequestService.getAbsenceRequests(
            eq(1L),
            eq(true),
            eq(RequestStatus.PENDING),
            any(AbsenceRequestPaginationRequest.class)
        )).willReturn(new PaginationResponse<>(new PageImpl<>(
            List.of(response),
            PageRequest.of(0, 20),
            35
        )));

        AdminPage<AbsenceRequestResponse> page = absenceRequestAdminViewService.getAbsenceRequests(
            1L,
            new AbsenceRequestFilter(RequestStatus.PENDING, "개인 사정", 0, 20)
        );

        assertThat(page.content()).containsExactly(response);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(35);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void getAbsenceRequests_normalizesInvalidPageAndSize() {
        given(absenceRequestService.getAbsenceRequests(
            eq(1L),
            eq(true),
            eq(null),
            any(AbsenceRequestPaginationRequest.class)
        )).willReturn(new PaginationResponse<>(new PageImpl<>(
            List.of(),
            PageRequest.of(0, 10),
            0
        )));
        ArgumentCaptor<AbsenceRequestPaginationRequest> captor =
            ArgumentCaptor.forClass(AbsenceRequestPaginationRequest.class);

        absenceRequestAdminViewService.getAbsenceRequests(
            1L,
            new AbsenceRequestFilter(null, "검색어", -1, 0)
        );

        then(absenceRequestService).should()
            .getAbsenceRequests(eq(1L), eq(true), eq(null), captor.capture());
        AbsenceRequestPaginationRequest pageRequest = captor.getValue();
        assertThat(pageRequest.getPage()).isZero();
        assertThat(pageRequest.getSize()).isEqualTo(10);
        assertThat(pageRequest.getKeyword()).isEqualTo("검색어");
    }

    @Test
    void getStatusLabel_returnsKoreanLabel() {
        assertThat(absenceRequestAdminViewService.getStatusLabel(RequestStatus.PENDING)).isEqualTo("승인 대기");
        assertThat(absenceRequestAdminViewService.getStatusLabel(RequestStatus.APPROVED)).isEqualTo("승인");
        assertThat(absenceRequestAdminViewService.getStatusLabel(RequestStatus.REJECTED)).isEqualTo("반려");
        assertThat(absenceRequestAdminViewService.getStatusLabel(RequestStatus.CANCELLED)).isEqualTo("취소");
        assertThat(absenceRequestAdminViewService.getStatusLabel(RequestStatus.EXPIRED)).isEqualTo("만료");
    }

    @Test
    void approve_delegatesToAbsenceRequestService() {
        absenceRequestAdminViewService.approve(1L, 10L);

        then(absenceRequestService).should()
            .approveAbsenceRequest(1L, 10L);
    }

    @Test
    void reject_delegatesToAbsenceRequestService() {
        absenceRequestAdminViewService.reject(1L, 10L, "사유가 충분하지 않습니다.");

        then(absenceRequestService).should()
            .rejectAbsenceRequest(1L, 10L, "사유가 충분하지 않습니다.");
    }

    private AbsenceRequestResponse absenceRequestResponse(Long id, RequestStatus status) {
        return new AbsenceRequestResponse(
            id,
            20L,
            LocalDate.of(2026, 5, 20),
            30L,
            "벚꽃반",
            40L,
            "김교사",
            "결석 요청",
            "개인 사정",
            LocalDateTime.of(2026, 5, 20, 0, 0),
            status,
            null,
            null,
            null,
            LocalDateTime.of(2026, 5, 18, 10, 0)
        );
    }
}
