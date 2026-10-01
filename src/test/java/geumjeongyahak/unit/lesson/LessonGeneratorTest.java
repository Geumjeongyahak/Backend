package geumjeongyahak.unit.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.service.schedule.LessonGenerator;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker.ConflictExclusion;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.users.entity.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LessonGeneratorTest {

    private static final LocalTime START = LocalTime.of(13, 0);
    private static final LocalTime END = LocalTime.of(13, 30);

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private TeacherLessonConflictChecker conflictChecker;

    @InjectMocks
    private LessonGenerator generator;

    @Test
    void lessonDates_includesBothEndsWhenTheyFallOnTheDay() {
        // 2099-03-02(월) ~ 2099-03-16(월)
        List<LocalDate> dates = LessonGenerator.lessonDates(
            LocalDate.of(2099, 3, 2), LocalDate.of(2099, 3, 16), DayOfWeek.MONDAY
        );

        assertThat(dates).containsExactly(
            LocalDate.of(2099, 3, 2), LocalDate.of(2099, 3, 9), LocalDate.of(2099, 3, 16)
        );
    }

    @Test
    void lessonDates_isEmptyWhenPeriodHasNoSuchDay() {
        // 2099-03-03(화) ~ 2099-03-08(일)
        assertThat(LessonGenerator.lessonDates(
            LocalDate.of(2099, 3, 3), LocalDate.of(2099, 3, 8), DayOfWeek.MONDAY
        )).isEmpty();
    }

    @Test
    void lessonDates_isEmptyWhenStartIsAfterEnd() {
        assertThat(LessonGenerator.lessonDates(
            LocalDate.of(2099, 3, 16), LocalDate.of(2099, 3, 2), DayOfWeek.MONDAY
        )).isEmpty();
    }

    @Test
    void generate_createsLessonOnEveryDateExceptConflictingOnes() {
        User teacher = teacher();
        Subject subject = subject(teacher);
        LocalDate conflictDate = LocalDate.of(2099, 3, 9);
        given(conflictChecker.findConflictDates(eq(2L), anyList(), eq(START), eq(END), eq(ConflictExclusion.NONE)))
            .willReturn(Set.of(conflictDate));
        given(lessonRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        List<Lesson> created = generator.generate(
            subject, teacher, LocalDate.of(2099, 3, 2), LocalDate.of(2099, 3, 16), DayOfWeek.MONDAY, START, END, 3
        );

        assertThat(created)
            .extracting(Lesson::getDate)
            .containsExactly(LocalDate.of(2099, 3, 2), LocalDate.of(2099, 3, 16));
        assertThat(created).allSatisfy(lesson -> {
            assertThat(lesson.getStartTime()).isEqualTo(START);
            assertThat(lesson.getEndTime()).isEqualTo(END);
            assertThat(lesson.getTeacher()).isSameAs(teacher);
        });
    }

    @Test
    void generate_savesNothingWhenNoDateInPeriod() {
        User teacher = teacher();
        given(conflictChecker.findConflictDates(eq(2L), eq(List.of()), eq(START), eq(END), eq(ConflictExclusion.NONE)))
            .willReturn(Set.of());
        given(lessonRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        List<Lesson> created = generator.generate(
            subject(teacher), teacher, LocalDate.of(2099, 3, 3), LocalDate.of(2099, 3, 8), DayOfWeek.MONDAY, START, END, 3
        );

        assertThat(created).isEmpty();
        verify(lessonRepository).saveAll(List.of());
    }

    private User teacher() {
        User teacher = User.builder().name("교사").role(RoleType.VOLUNTEER).build();
        ReflectionTestUtils.setField(teacher, "id", 2L);
        return teacher;
    }

    private Subject subject(User teacher) {
        Subject subject = new Subject(
            null, teacher, "3교시", LocalDate.of(2099, 3, 2), LocalDate.of(2099, 6, 30),
            DayOfWeek.MONDAY, START, END, 3, null, null
        );
        ReflectionTestUtils.setField(subject, "id", 10L);
        return subject;
    }
}
