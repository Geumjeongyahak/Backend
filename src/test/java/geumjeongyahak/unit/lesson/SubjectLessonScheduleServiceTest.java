package geumjeongyahak.unit.lesson;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.service.schedule.DailyScheduleSyncPublisher;
import geumjeongyahak.domain.lesson.service.schedule.LessonGenerator;
import geumjeongyahak.domain.lesson.service.schedule.SubjectLessonScheduleService;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.users.service.UserProxyService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubjectLessonScheduleServiceTest {

    private static final LocalDate FROM = LocalDate.of(2099, 3, 2);

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private SubjectProxyService subjectProxyService;

    @Mock
    private UserProxyService userProxyService;

    @Mock
    private LessonGenerator lessonGenerator;

    @Mock
    private DailyScheduleSyncPublisher syncPublisher;

    @InjectMocks
    private SubjectLessonScheduleService service;

    @Test
    void recreateLessons_onlyDeletesWhenSubjectHasNoTeacher() {
        Lesson future = mock(Lesson.class);
        given(lessonRepository.findAllBySubjectIdAndStatusAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(
            10L, LessonStatus.SCHEDULED, FROM
        )).willReturn(List.of(future));

        service.recreateLessons(
            10L, null, FROM, FROM, FROM.plusWeeks(4), DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(13, 30), 3
        );

        verify(future).softDelete();
        verify(syncPublisher).publishFor(List.of(future));
        verifyNoInteractions(lessonGenerator, userProxyService, subjectProxyService);
    }

    @Test
    void deleteFutureLessons_publishesOnceEvenWhenThereIsNothingToDelete() {
        given(lessonRepository.findAllBySubjectIdAndStatusAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(
            10L, LessonStatus.SCHEDULED, FROM
        )).willReturn(List.of());

        service.deleteFutureLessons(10L, FROM);

        verify(syncPublisher).publishFor(List.of());
        verifyNoInteractions(lessonGenerator);
    }
}
