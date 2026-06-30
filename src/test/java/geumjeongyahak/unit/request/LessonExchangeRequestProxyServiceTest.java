package geumjeongyahak.unit.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.repository.LessonExchangeProposalRepository;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.domain.request.service.LessonExchangeRequestProxyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonExchangeRequestProxyServiceTest {

    private static final Long USER_ID = 10L;
    private static final List<LessonExchangeRequestStatus> ACTIVE_REQUEST_STATUSES = List.of(
        LessonExchangeRequestStatus.PENDING,
        LessonExchangeRequestStatus.APPROVED
    );

    @Mock
    private LessonExchangeRequestRepository lessonExchangeRequestRepository;

    @Mock
    private LessonExchangeProposalRepository lessonExchangeProposalRepository;

    @InjectMocks
    private LessonExchangeRequestProxyService lessonExchangeRequestProxyService;

    @Test
    void existsActiveExchangeByUserId_checksPendingAndApprovedRequests() {
        given(lessonExchangeRequestRepository.existsByRequestedBy_IdAndStatusIn(
            USER_ID,
            ACTIVE_REQUEST_STATUSES
        )).willReturn(true);

        boolean exists = lessonExchangeRequestProxyService.existsActiveExchangeByUserId(USER_ID);

        assertThat(exists).isTrue();
        verify(lessonExchangeRequestRepository).existsByRequestedBy_IdAndStatusIn(
            USER_ID,
            ACTIVE_REQUEST_STATUSES
        );
    }

    @Test
    void existsActiveExchangeByUserId_checksActiveProposals() {
        given(lessonExchangeProposalRepository.existsByProposedBy_IdAndStatus(
            USER_ID,
            LessonExchangeProposalStatus.ACTIVE
        )).willReturn(true);

        boolean exists = lessonExchangeRequestProxyService.existsActiveExchangeByUserId(USER_ID);

        assertThat(exists).isTrue();
        verify(lessonExchangeProposalRepository).existsByProposedBy_IdAndStatus(
            USER_ID,
            LessonExchangeProposalStatus.ACTIVE
        );
    }
}
