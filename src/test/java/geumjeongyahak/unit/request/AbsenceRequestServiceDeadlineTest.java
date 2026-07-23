package geumjeongyahak.unit.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleProxyService;
import geumjeongyahak.domain.request.entity.AbsenceRequest;
import geumjeongyahak.domain.request.enums.RequestStatus;
import geumjeongyahak.domain.request.repository.AbsenceRequestRepository;
import geumjeongyahak.domain.request.service.AbsenceRequestService;
import geumjeongyahak.domain.request.v1.dto.request.CreateAbsenceRequestRequest;
import geumjeongyahak.domain.request.v1.dto.request.UpdateAbsenceRequestRequest;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AbsenceRequestServiceDeadlineTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate LESSON_DATE = LocalDate.of(2026, 7, 23);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 23, 8, 0);
    private static final LocalDateTime LESSON_START = LocalDateTime.of(2026, 7, 23, 9, 0);

    @Mock
    private AbsenceRequestRepository absenceRequestRepository;
    @Mock
    private DailyScheduleProxyService dailyScheduleProxyService;
    @Mock
    private UserProxyService userProxyService;
    @Mock
    private EventPublisher eventPublisher;

    private User teacher;
    private DailySchedule dailySchedule;
    private AbsenceRequestService service;

    @BeforeEach
    void setUp() {
        Classroom classroom = Classroom.builder()
            .name("벚꽃반")
            .type(ClassroomType.WEEKDAY)
            .build();
        ReflectionTestUtils.setField(classroom, "id", 10L);

        teacher = User.builder()
            .name("홍길동")
            .role(RoleType.VOLUNTEER)
            .build();
        ReflectionTestUtils.setField(teacher, "id", 20L);

        dailySchedule = new DailySchedule(
            classroom,
            teacher,
            LESSON_DATE,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
        );
        ReflectionTestUtils.setField(dailySchedule, "id", 30L);

        service = createServiceAt(NOW);
    }

    @Test
    void create_sameDayBeforeLessonStart_defaultsExpiresAtToLessonStart() {
        prepareCreate();
        prepareSuccessfulSave();

        var response = service.createAbsenceRequest(
            teacher.getId(),
            new CreateAbsenceRequestRequest(LESSON_DATE, "결석 요청", "개인 사정")
        );

        assertThat(response.expiresAt()).isEqualTo(LESSON_START);
        then(absenceRequestRepository).should().save(any(AbsenceRequest.class));
    }

    @Test
    void create_afterLessonStart_throwsBadRequest() {
        prepareCreate();
        service = createServiceAt(LESSON_START);

        assertBusinessException(
            () -> service.createAbsenceRequest(
                teacher.getId(),
                new CreateAbsenceRequestRequest(LESSON_DATE, "결석 요청", "개인 사정")
            ),
            "REQ-07-023"
        );
    }

    @Test
    void create_withoutLessonStartTime_throwsConflict() {
        ReflectionTestUtils.setField(dailySchedule, "activityStartTime", null);
        prepareCreate();

        assertBusinessException(
            () -> service.createAbsenceRequest(
                teacher.getId(),
                new CreateAbsenceRequestRequest(LESSON_DATE, "결석 요청", "개인 사정")
            ),
            "REQ-07-025"
        );
    }

    @Test
    void update_preservesAutomaticallyAssignedExpiresAt() {
        LocalDateTime originalExpiresAt = NOW.plusMinutes(20);
        AbsenceRequest request = pendingRequest(originalExpiresAt);
        given(absenceRequestRepository.findById(request.getId())).willReturn(Optional.of(request));

        var response = service.updateAbsenceRequest(
            teacher.getId(),
            request.getId(),
            new UpdateAbsenceRequestRequest("수정 제목", "수정 사유")
        );

        assertThat(response.expiresAt()).isEqualTo(originalExpiresAt);
    }

    @Test
    void update_afterLessonStart_throwsBadRequestAndPreservesRequest() {
        AbsenceRequest request = pendingRequest(LESSON_START);
        given(absenceRequestRepository.findById(request.getId())).willReturn(Optional.of(request));
        service = createServiceAt(LESSON_START);

        assertBusinessException(
            () -> service.updateAbsenceRequest(
                teacher.getId(),
                request.getId(),
                new UpdateAbsenceRequestRequest("수정 제목", "수정 사유")
            ),
            "REQ-07-023"
        );
        assertThat(request.getTitle()).isEqualTo("결석 요청");
        assertThat(request.getReason()).isEqualTo("개인 사정");
        assertThat(request.getExpiresAt()).isEqualTo(LESSON_START);
    }

    @Test
    void approve_atExpiresAt_rechecksDeadlineAndThrowsConflict() {
        AbsenceRequest request = pendingRequest(NOW);
        given(absenceRequestRepository.findById(request.getId())).willReturn(Optional.of(request));

        assertBusinessException(
            () -> service.approveAbsenceRequest(99L, request.getId()),
            "REQ-07-024"
        );
        then(userProxyService).shouldHaveNoMoreInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void reject_atLessonStart_throwsConflictAndPreservesRequest() {
        AbsenceRequest request = pendingRequest(LESSON_START);
        given(absenceRequestRepository.findById(request.getId())).willReturn(Optional.of(request));
        service = createServiceAt(LESSON_START);

        assertBusinessException(
            () -> service.rejectAbsenceRequest(99L, request.getId(), "반려 사유"),
            "REQ-07-024"
        );
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(request.getApprovalAt()).isNull();
        assertThat(request.getApprovalBy()).isNull();
        assertThat(request.getNote()).isNull();
        then(userProxyService).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void delete_atLessonStart_throwsConflictAndPreservesRequest() {
        AbsenceRequest request = pendingRequest(LESSON_START);
        given(absenceRequestRepository.findById(request.getId())).willReturn(Optional.of(request));
        service = createServiceAt(LESSON_START);

        assertBusinessException(
            () -> service.deleteAbsenceRequest(teacher.getId(), request.getId()),
            "REQ-07-024"
        );
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void expire_atExactExpiresAt_marksPendingRequestExpired() {
        AbsenceRequest request = pendingRequest(NOW);
        given(absenceRequestRepository.findAllByStatusInAndExpiresAtLessThanEqual(
            List.of(RequestStatus.PENDING),
            NOW
        )).willReturn(List.of(request));

        int expiredCount = service.expireExpiredAbsenceRequests();

        assertThat(expiredCount).isOne();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.EXPIRED);
        ArgumentCaptor<LocalDateTime> expiresAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        then(absenceRequestRepository).should().findAllByStatusInAndExpiresAtLessThanEqual(
            eq(List.of(RequestStatus.PENDING)),
            expiresAtCaptor.capture()
        );
        assertThat(expiresAtCaptor.getValue()).isEqualTo(NOW);
    }

    private void prepareCreate() {
        given(dailyScheduleProxyService.getActiveByTeacherIdAndLessonDate(
            teacher.getId(),
            LESSON_DATE
        )).willReturn(dailySchedule);
        given(userProxyService.getById(teacher.getId())).willReturn(teacher);
    }

    private void prepareSuccessfulSave() {
        given(absenceRequestRepository.save(any(AbsenceRequest.class)))
            .willAnswer(invocation -> {
                AbsenceRequest request = invocation.getArgument(0);
                ReflectionTestUtils.setField(request, "id", 40L);
                return request;
            });
    }

    private AbsenceRequest pendingRequest(LocalDateTime expiresAt) {
        AbsenceRequest request = new AbsenceRequest(
            dailySchedule,
            teacher,
            "결석 요청",
            "개인 사정",
            expiresAt
        );
        ReflectionTestUtils.setField(request, "id", 40L);
        return request;
    }

    private AbsenceRequestService createServiceAt(LocalDateTime dateTime) {
        Clock clock = Clock.fixed(dateTime.atZone(SEOUL).toInstant(), SEOUL);
        return new AbsenceRequestService(
            absenceRequestRepository,
            dailyScheduleProxyService,
            userProxyService,
            eventPublisher,
            clock
        );
    }

    private void assertBusinessException(Runnable action, String expectedCode) {
        assertThatThrownBy(action::run)
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getCode())
            .isEqualTo(expectedCode);
    }
}
