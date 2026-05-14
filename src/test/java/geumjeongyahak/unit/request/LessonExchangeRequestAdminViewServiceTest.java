package geumjeongyahak.unit.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.repository.LessonExchangeProposalRepository;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.domain.request.service.LessonExchangeRequestAdminViewService;
import geumjeongyahak.domain.request.service.LessonExchangeRequestAdminViewService.LessonExchangeDashboard;
import geumjeongyahak.domain.request.service.LessonExchangeRequestAdminViewService.ReviewRequiredRequestRow;
import geumjeongyahak.domain.request.service.LessonExchangeRequestAdminViewService.StatusCount;
import geumjeongyahak.domain.users.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LessonExchangeRequestAdminViewServiceTest {

    @Mock
    private LessonExchangeRequestRepository lessonExchangeRequestRepository;

    @Mock
    private LessonExchangeProposalRepository lessonExchangeProposalRepository;

    @InjectMocks
    private LessonExchangeRequestAdminViewService lessonExchangeRequestAdminViewService;

    @Test
    void getDashboard_countsRequestAndProposalStatuses() {
        for (LessonExchangeRequestStatus status : LessonExchangeRequestStatus.values()) {
            given(lessonExchangeRequestRepository.countByStatus(status)).willReturn(0L);
        }
        for (LessonExchangeProposalStatus status : LessonExchangeProposalStatus.values()) {
            given(lessonExchangeProposalRepository.countByStatus(status)).willReturn(0L);
        }
        given(lessonExchangeRequestRepository.countByStatus(LessonExchangeRequestStatus.PENDING))
            .willReturn(3L);
        given(lessonExchangeRequestRepository.countByStatus(LessonExchangeRequestStatus.APPROVED))
            .willReturn(2L);
        given(lessonExchangeProposalRepository.countByStatus(LessonExchangeProposalStatus.ACTIVE))
            .willReturn(5L);
        given(lessonExchangeProposalRepository.countByStatus(LessonExchangeProposalStatus.ACCEPTED))
            .willReturn(1L);
        given(lessonExchangeRequestRepository.findTop10ByStatusOrderByCreatedAtAsc(LessonExchangeRequestStatus.PENDING))
            .willReturn(List.of());

        LessonExchangeDashboard dashboard = lessonExchangeRequestAdminViewService.getDashboard();

        assertThat(dashboard.pendingRequestCount()).isEqualTo(3L);
        assertThat(dashboard.activeProposalCount()).isEqualTo(5L);
        assertThat(findRequestStatusCount(dashboard, LessonExchangeRequestStatus.APPROVED).count())
            .isEqualTo(2L);
        assertThat(findProposalStatusCount(dashboard, LessonExchangeProposalStatus.ACCEPTED).count())
            .isEqualTo(1L);
    }

    @Test
    void getDashboard_includesReviewRequiredRequestsWithProposalCounts() {
        LessonExchangeRequest request = lessonExchangeRequest(
            10L,
            "벚꽃반",
            "김교사",
            "6월 10일 수업 교환 요청",
            LocalDate.of(2026, 6, 10),
            LocalDateTime.of(2026, 6, 7, 23, 59),
            LocalDateTime.of(2026, 5, 20, 10, 0)
        );
        given(lessonExchangeRequestRepository.findTop10ByStatusOrderByCreatedAtAsc(LessonExchangeRequestStatus.PENDING))
            .willReturn(List.of(request));
        given(lessonExchangeProposalRepository.countByRequest_Id(10L)).willReturn(4L);
        given(lessonExchangeProposalRepository.countByRequest_IdAndStatus(10L, LessonExchangeProposalStatus.ACTIVE))
            .willReturn(2L);

        LessonExchangeDashboard dashboard = lessonExchangeRequestAdminViewService.getDashboard();

        assertThat(dashboard.reviewRequiredRequests()).hasSize(1);
        ReviewRequiredRequestRow row = dashboard.reviewRequiredRequests().getFirst();
        assertThat(row.id()).isEqualTo(10L);
        assertThat(row.classroomName()).isEqualTo("벚꽃반");
        assertThat(row.requestedByName()).isEqualTo("김교사");
        assertThat(row.status()).isEqualTo(LessonExchangeRequestStatus.PENDING);
        assertThat(row.statusLabel()).isEqualTo("승인 대기");
        assertThat(row.proposalCount()).isEqualTo(4L);
        assertThat(row.activeProposalCount()).isEqualTo(2L);
    }

    private StatusCount<LessonExchangeRequestStatus> findRequestStatusCount(
        LessonExchangeDashboard dashboard,
        LessonExchangeRequestStatus status
    ) {
        return dashboard.requestStatusCounts().stream()
            .filter(statusCount -> statusCount.status() == status)
            .findFirst()
            .orElseThrow();
    }

    private StatusCount<LessonExchangeProposalStatus> findProposalStatusCount(
        LessonExchangeDashboard dashboard,
        LessonExchangeProposalStatus status
    ) {
        return dashboard.proposalStatusCounts().stream()
            .filter(statusCount -> statusCount.status() == status)
            .findFirst()
            .orElseThrow();
    }

    private LessonExchangeRequest lessonExchangeRequest(
        Long id,
        String classroomName,
        String requestedByName,
        String title,
        LocalDate lessonDate,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
    ) {
        User requestedBy = User.builder()
            .nickname(requestedByName)
            .name(requestedByName)
            .role(RoleType.VOLUNTEER)
            .build();
        LessonExchangeRequest request = new LessonExchangeRequest(
            requestedBy,
            lessonDate,
            title,
            classroomName,
            "수업 교환을 요청합니다.",
            expiresAt
        );
        ReflectionTestUtils.setField(request, "id", id);
        ReflectionTestUtils.setField(request, "createdAt", createdAt);
        return request;
    }
}
