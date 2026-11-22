package geumjeongyahak.unit.subject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.exception.SubjectCopyConflictException;
import geumjeongyahak.domain.subject.exception.SubjectDuplicateException;
import geumjeongyahak.domain.subject.exception.SubjectNotCopyableException;
import geumjeongyahak.domain.subject.repository.SubjectRepository;
import geumjeongyahak.domain.subject.service.SubjectCopyService;
import geumjeongyahak.domain.subject.service.SubjectScheduleValidator;
import geumjeongyahak.domain.subject.v1.dto.request.SubjectCopyRequest;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubjectCopyServiceTest {

    private static final LocalDate START = LocalDate.of(2099, 4, 1);
    private static final LocalDate END = LocalDate.of(2099, 4, 30);

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private EventPublisher eventPublisher;

    /** 판정 규칙은 E2E가 본다. 여기서는 검증기가 거절했을 때 «사유를 모으고 저장하지 않는지»만 본다. */
    @Mock
    private SubjectScheduleValidator validator;

    private SubjectCopyService service;

    @BeforeEach
    void setUp() {
        service = new SubjectCopyService(
            subjectRepository,
            validator,
            eventPublisher,
            Clock.fixed(Instant.parse("2099-03-15T00:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    private Subject subject(long id, long classroomId, LocalTime start, LocalTime end, int period) {
        Classroom classroom = Classroom.builder().name("반" + classroomId).type(ClassroomType.WEEKDAY).build();
        ReflectionTestUtils.setField(classroom, "id", classroomId);
        Subject subject = new Subject(
            classroom, null, "과목" + id, LocalDate.of(2099, 3, 1), LocalDate.of(2099, 3, 31),
            DayOfWeek.MONDAY, start, end, period, null, null
        );
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "isActive", true);
        return subject;
    }

    @Test
    void validatorRejection_isCollectedWithItsMessage_andNothingIsSaved() {
        Subject ok = subject(1L, 1L, LocalTime.of(19, 20), LocalTime.of(20, 0), 1);
        Subject taken = subject(2L, 2L, LocalTime.of(19, 20), LocalTime.of(20, 0), 1);
        given(subjectRepository.findAllByIdIn(any())).willReturn(List.of(ok, taken));
        lenient().doThrow(new SubjectDuplicateException("이미 과목이 있음"))
            .when(validator).validateSubjectDuplicate(null, 2L, DayOfWeek.MONDAY, START, END, LocalTime.of(19, 20), LocalTime.of(20, 0));

        assertThatThrownBy(() -> service.copy(new SubjectCopyRequest(List.of(1L, 2L), START, END)))
            .isInstanceOf(SubjectCopyConflictException.class)
            .extracting(e -> ((SubjectCopyConflictException) e).problemProperties().get("failures"), list(Object.class))
            .singleElement()
            .hasFieldOrPropertyWithValue("sourceSubjectId", 2L)
            .hasFieldOrPropertyWithValue("reason", "이미 과목이 있음");
        verify(subjectRepository, never()).saveAll(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void requestedSubjectsInTheSameCellAndTime_failTogether() {
        Subject spring = subject(1L, 1L, LocalTime.of(19, 20), LocalTime.of(20, 0), 1);
        Subject march = subject(2L, 1L, LocalTime.of(19, 30), LocalTime.of(20, 10), 1);
        Subject otherCell = subject(3L, 2L, LocalTime.of(19, 20), LocalTime.of(20, 0), 1);
        given(subjectRepository.findAllByIdIn(any())).willReturn(List.of(spring, march, otherCell));

        assertThatThrownBy(() -> service.copy(new SubjectCopyRequest(List.of(1L, 2L, 3L), START, END)))
            .isInstanceOf(SubjectCopyConflictException.class)
            .extracting(e -> ((SubjectCopyConflictException) e).problemProperties().get("failures"), list(Object.class))
            .extracting("sourceSubjectId")
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void touchingTimesInTheSameCell_doNotCollide() {
        Subject second = subject(1L, 1L, LocalTime.of(12, 20), LocalTime.of(13, 0), 2);
        Subject third = subject(2L, 1L, LocalTime.of(13, 0), LocalTime.of(13, 30), 3);
        given(subjectRepository.findAllByIdIn(any())).willReturn(List.of(second, third));
        given(subjectRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.copy(new SubjectCopyRequest(List.of(1L, 2L), START, END)).copiedCount()).isEqualTo(2);
    }

    @Test
    void duplicateIds_badRequest() {
        assertThatThrownBy(() -> service.copy(new SubjectCopyRequest(List.of(1L, 1L), START, END)))
            .isInstanceOf(SubjectNotCopyableException.class);
        verifyNoInteractions(subjectRepository);
    }

    @Test
    void missingOrInactiveIds_badRequestNamingThem() {
        Subject inactive = subject(2L, 1L, LocalTime.of(19, 20), LocalTime.of(20, 0), 1);
        ReflectionTestUtils.setField(inactive, "isActive", false);
        given(subjectRepository.findAllByIdIn(any())).willReturn(List.of(inactive));

        assertThatThrownBy(() -> service.copy(new SubjectCopyRequest(List.of(1L, 2L), START, END)))
            .isInstanceOf(SubjectNotCopyableException.class)
            .hasMessageContaining("[1, 2]");
    }

    @Test
    void sameCellCollisions_collectEveryParticipant_notOnlyNeighbours() {
        Subject long1 = subject(1L, 1L, LocalTime.of(9, 0), LocalTime.of(12, 0), 1);
        Subject inner = subject(2L, 1L, LocalTime.of(10, 0), LocalTime.of(11, 0), 2);
        Subject late = subject(3L, 1L, LocalTime.of(11, 30), LocalTime.of(13, 0), 3);
        given(subjectRepository.findAllByIdIn(any())).willReturn(List.of(long1, inner, late));

        assertThatThrownBy(() -> service.copy(new SubjectCopyRequest(List.of(1L, 2L, 3L), START, END)))
            .isInstanceOf(SubjectCopyConflictException.class)
            .extracting(e -> ((SubjectCopyConflictException) e).problemProperties().get("failures"), list(Object.class))
            .extracting("sourceSubjectId")
            .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void sameCellOverlap_isNotACollision_whenTargetPeriodHasNoSuchWeekday() {
        Subject first = subject(1L, 1L, LocalTime.of(19, 20), LocalTime.of(20, 0), 1);
        Subject overlapping = subject(2L, 1L, LocalTime.of(19, 30), LocalTime.of(20, 10), 2);
        given(subjectRepository.findAllByIdIn(any())).willReturn(List.of(first, overlapping));
        given(subjectRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
        LocalDate tuesday = LocalDate.of(2099, 4, 7);

        assertThat(service.copy(new SubjectCopyRequest(List.of(1L, 2L), tuesday, tuesday)).copiedCount()).isEqualTo(2);
    }
}
