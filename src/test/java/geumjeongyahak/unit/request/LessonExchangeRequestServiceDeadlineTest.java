package geumjeongyahak.unit.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleProxyService;
import geumjeongyahak.domain.request.entity.LessonExchangeRequest;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.repository.LessonExchangeRequestRepository;
import geumjeongyahak.domain.request.service.LessonExchangeRequestService;
import geumjeongyahak.domain.request.v1.dto.request.CreateLessonExchangeRequestRequest;
import geumjeongyahak.domain.request.v1.dto.request.UpdateLessonExchangeRequestRequest;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LessonExchangeRequestServiceDeadlineTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate LESSON_DATE = LocalDate.of(2026, 7, 23);
    private static final LocalDateTime NOW = LESSON_DATE.atTime(8, 0);
    private static final LocalDateTime LESSON_START = LESSON_DATE.atTime(9, 0);

    @Mock
    private LessonExchangeRequestRepository requestRepository;
    @Mock
    private DailyScheduleProxyService dailyScheduleProxyService;
    @Mock
    private UserProxyService userProxyService;
    @Mock
    private EventPublisher eventPublisher;

    private User teacher;
    private User approver;
    private DailySchedule dailySchedule;
    private LessonExchangeRequestService service;

    @BeforeEach
    void setUp() {
        Classroom classroom = Classroom.builder()
            .name("벚꽃반")
            .type(ClassroomType.WEEKDAY)
            .build();
        ReflectionTestUtils.setField(classroom, "id", 10L);

        teacher = User.builder().name("홍길동").role(RoleType.VOLUNTEER).build();
        ReflectionTestUtils.setField(teacher, "id", 20L);
        approver = User.builder().name("관리자").role(RoleType.ADMIN).build();
        ReflectionTestUtils.setField(approver, "id", 99L);

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
    void create_withoutExpiresAt_defaultsToLessonStart() {
        prepareCreate();
        prepareSuccessfulSave();

        var response = service.createLessonExchangeRequest(
            teacher.getId(),
            new CreateLessonExchangeRequestRequest(LESSON_DATE, "교환 요청", "개인 사정", null)
        );

        assertThat(response.expiresAt()).isEqualTo(LESSON_START);
    }

    @Test
    void create_atLessonStart_throwsBadRequest() {
        prepareCreate();
        service = createServiceAt(LESSON_START);

        assertBusinessException(
            () -> service.createLessonExchangeRequest(
                teacher.getId(),
                new CreateLessonExchangeRequestRequest(LESSON_DATE, "교환 요청", "개인 사정", null)
            ),
            "REQ-07-026"
        );
    }

    @Test
    void create_withExpiresAtEqualToLessonStart_succeeds() {
        prepareCreate();
        prepareSuccessfulSave();

        var response = service.createLessonExchangeRequest(
            teacher.getId(),
            new CreateLessonExchangeRequestRequest(
                LESSON_DATE, "교환 요청", "개인 사정", LESSON_DATE
            )
        );

        assertThat(response.expiresAt()).isEqualTo(LESSON_START);
    }

    @Test
    void create_withEarlierExpiresDate_usesEndOfSelectedDate() {
        prepareCreate();
        prepareSuccessfulSave();
        service = createServiceAt(LESSON_DATE.minusDays(2).atTime(8, 0));

        var response = service.createLessonExchangeRequest(
            teacher.getId(),
            new CreateLessonExchangeRequestRequest(
                LESSON_DATE,
                "교환 요청",
                "개인 사정",
                LESSON_DATE.minusDays(1)
            )
        );

        assertThat(response.expiresAt())
            .isEqualTo(LESSON_DATE.minusDays(1).atTime(23, 59, 59));
    }

    @Test
    void create_withCurrentDateForFutureLesson_usesEndOfCurrentDate() {
        LocalDate currentDate = LESSON_DATE.minusDays(1);
        prepareCreate();
        prepareSuccessfulSave();
        service = createServiceAt(currentDate.atTime(12, 0));

        var response = service.createLessonExchangeRequest(
            teacher.getId(),
            new CreateLessonExchangeRequestRequest(
                LESSON_DATE,
                "교환 요청",
                "개인 사정",
                currentDate
            )
        );

        assertThat(response.expiresAt())
            .isEqualTo(currentDate.atTime(23, 59, 59));
    }

    @Test
    void create_withPastExpiresDate_throwsBadRequest() {
        LocalDate currentDate = LESSON_DATE.minusDays(1);
        prepareCreate();
        service = createServiceAt(currentDate.atTime(12, 0));

        assertBusinessException(
            () -> service.createLessonExchangeRequest(
                teacher.getId(),
                new CreateLessonExchangeRequestRequest(
                    LESSON_DATE,
                    "교환 요청",
                    "개인 사정",
                    currentDate.minusDays(1)
                )
            ),
            "REQ-07-004"
        );
        then(userProxyService).shouldHaveNoInteractions();
        then(requestRepository).shouldHaveNoInteractions();
    }

    @Test
    void create_atExactEndOfSelectedExpiresDate_throwsBadRequest() {
        LocalDate currentDate = LESSON_DATE.minusDays(1);
        prepareCreate();
        service = createServiceAt(currentDate.atTime(23, 59, 59));

        assertBusinessException(
            () -> service.createLessonExchangeRequest(
                teacher.getId(),
                new CreateLessonExchangeRequestRequest(
                    LESSON_DATE,
                    "교환 요청",
                    "개인 사정",
                    currentDate
                )
            ),
            "REQ-07-004"
        );
        then(userProxyService).shouldHaveNoInteractions();
        then(requestRepository).shouldHaveNoInteractions();
    }

    @Test
    void create_withExpiresDateAfterLessonDate_throwsBadRequest() {
        prepareCreate();

        assertBusinessException(
            () -> service.createLessonExchangeRequest(
                teacher.getId(),
                new CreateLessonExchangeRequestRequest(
                    LESSON_DATE, "교환 요청", "개인 사정", LESSON_DATE.plusDays(1)
                )
            ),
            "REQ-07-005"
        );
    }

    @Test
    void create_withoutLessonStartTime_throwsConflict() {
        ReflectionTestUtils.setField(dailySchedule, "activityStartTime", null);
        prepareCreate();

        assertBusinessException(
            () -> service.createLessonExchangeRequest(
                teacher.getId(),
                new CreateLessonExchangeRequestRequest(LESSON_DATE, "교환 요청", "개인 사정", null)
            ),
            "REQ-07-028"
        );
    }

    @Test
    void update_withoutExpiresAt_defaultsToSelectedLessonStart() {
        LessonExchangeRequest request = pendingRequest(NOW.plusMinutes(10));
        given(requestRepository.findById(request.getId())).willReturn(Optional.of(request));
        given(dailyScheduleProxyService.getActiveByTeacherIdAndLessonDate(
            teacher.getId(), LESSON_DATE
        )).willReturn(dailySchedule);

        var response = service.updateLessonExchangeRequest(
            teacher.getId(),
            request.getId(),
            new UpdateLessonExchangeRequestRequest(LESSON_DATE, "수정 제목", "수정 내용", null)
        );

        assertThat(response.expiresAt()).isEqualTo(LESSON_START);
    }

    @Test
    void update_atLessonStart_throwsBadRequestAndPreservesRequest() {
        LessonExchangeRequest request = pendingRequest(LESSON_START);
        given(requestRepository.findById(request.getId())).willReturn(Optional.of(request));
        service = createServiceAt(LESSON_START);

        assertBusinessException(
            () -> service.updateLessonExchangeRequest(
                teacher.getId(),
                request.getId(),
                new UpdateLessonExchangeRequestRequest(LESSON_DATE, "수정 제목", "수정 내용", null)
            ),
            "REQ-07-026"
        );
        assertThat(request.getTitle()).isEqualTo("교환 요청");
        assertThat(request.getContent()).isEqualTo("개인 사정");
    }

    @Test
    void update_afterRequestExpires_throwsConflictAndPreservesRequest() {
        LessonExchangeRequest request = pendingRequest(NOW);
        given(requestRepository.findById(request.getId())).willReturn(Optional.of(request));

        assertBusinessException(
            () -> service.updateLessonExchangeRequest(
                teacher.getId(),
                request.getId(),
                new UpdateLessonExchangeRequestRequest(LESSON_DATE, "수정 제목", "수정 내용", LESSON_DATE)
            ),
            "REQ-07-027"
        );
        assertThat(request.getTitle()).isEqualTo("교환 요청");
        assertThat(request.getContent()).isEqualTo("개인 사정");
        then(dailyScheduleProxyService).shouldHaveNoInteractions();
    }

    @Test
    void approve_atExpiresAt_throwsConflictAndPreservesRequest() {
        LessonExchangeRequest request = pendingRequest(NOW);
        given(requestRepository.findById(request.getId())).willReturn(Optional.of(request));

        assertBusinessException(
            () -> service.approveLessonExchangeRequest(approver.getId(), request.getId()),
            "REQ-07-027"
        );
        assertThat(request.getStatus()).isEqualTo(LessonExchangeRequestStatus.PENDING);
        then(userProxyService).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void reject_atLessonStart_throwsConflictAndPreservesRequest() {
        LessonExchangeRequest request = pendingRequest(LESSON_START);
        given(requestRepository.findById(request.getId())).willReturn(Optional.of(request));
        service = createServiceAt(LESSON_START);

        assertBusinessException(
            () -> service.rejectLessonExchangeRequest(approver.getId(), request.getId(), "반려"),
            "REQ-07-027"
        );
        assertThat(request.getStatus()).isEqualTo(LessonExchangeRequestStatus.PENDING);
        then(userProxyService).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void cancel_atLessonStart_throwsConflictAndPreservesRequest() {
        LessonExchangeRequest request = pendingRequest(LESSON_START);
        given(requestRepository.findById(request.getId())).willReturn(Optional.of(request));
        service = createServiceAt(LESSON_START);

        assertBusinessException(
            () -> service.cancelLessonExchangeRequest(teacher.getId(), request.getId()),
            "REQ-07-027"
        );
        assertThat(request.getStatus()).isEqualTo(LessonExchangeRequestStatus.PENDING);
    }

    @Test
    void expire_atExactExpiresAt_marksPendingRequestExpired() {
        LessonExchangeRequest request = pendingRequest(NOW);
        given(requestRepository.findAllByStatusInAndExpiresAtLessThanEqual(
            List.of(LessonExchangeRequestStatus.PENDING, LessonExchangeRequestStatus.APPROVED),
            NOW
        )).willReturn(List.of(request));

        int expiredCount = service.expireExpiredLessonExchangeRequests();

        assertThat(expiredCount).isOne();
        assertThat(request.getStatus()).isEqualTo(LessonExchangeRequestStatus.EXPIRED);
    }

    private void prepareCreate() {
        given(dailyScheduleProxyService.getActiveByTeacherIdAndLessonDate(
            teacher.getId(), LESSON_DATE
        )).willReturn(dailySchedule);
    }

    private void prepareSuccessfulSave() {
        given(userProxyService.getById(teacher.getId())).willReturn(teacher);
        given(requestRepository.save(any(LessonExchangeRequest.class)))
            .willAnswer(invocation -> {
                LessonExchangeRequest request = invocation.getArgument(0);
                ReflectionTestUtils.setField(request, "id", 40L);
                return request;
            });
    }

    private LessonExchangeRequest pendingRequest(LocalDateTime expiresAt) {
        LessonExchangeRequest request = new LessonExchangeRequest(
            dailySchedule,
            teacher,
            "교환 요청",
            dailySchedule.getClassroom().getName(),
            "개인 사정",
            expiresAt
        );
        ReflectionTestUtils.setField(request, "id", 40L);
        return request;
    }

    private LessonExchangeRequestService createServiceAt(LocalDateTime dateTime) {
        Clock clock = Clock.fixed(dateTime.atZone(SEOUL).toInstant(), SEOUL);
        return new LessonExchangeRequestService(
            requestRepository,
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
