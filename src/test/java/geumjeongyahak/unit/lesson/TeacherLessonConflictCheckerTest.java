package geumjeongyahak.unit.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker.ConflictExclusion;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker.TimeSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeacherLessonConflictCheckerTest {

    private static final LocalDate MONDAY = LocalDate.of(2099, 3, 2);

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private TeacherLessonConflictChecker checker;

    private static TimeSlot slot(LocalDate date, int startHour, int startMinute, int endHour, int endMinute) {
        return new TimeSlot(date, LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute));
    }

    @Test
    void findConflictDates_skipsQueryWhenNoDate() {
        assertThat(checker.findConflictDates(2L, List.of(), LocalTime.NOON, LocalTime.of(13, 0), ConflictExclusion.NONE))
            .isEmpty();
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void overlapAmong_touchingSlotsDoNotOverlap() {
        assertThat(TeacherLessonConflictChecker.overlapAmong(List.of(
            slot(MONDAY, 12, 20, 13, 0),
            slot(MONDAY, 13, 0, 13, 30),
            slot(MONDAY, 11, 30, 12, 10)
        ))).isFalse();
    }

    @Test
    void overlapAmong_oneMinuteOverlapOnSameDate() {
        assertThat(TeacherLessonConflictChecker.overlapAmong(List.of(
            slot(MONDAY, 12, 20, 13, 0),
            slot(MONDAY, 12, 59, 13, 30)
        ))).isTrue();
    }

    @Test
    void overlapAmong_containedSlotOverlaps() {
        assertThat(TeacherLessonConflictChecker.overlapAmong(List.of(
            slot(MONDAY, 10, 0, 14, 0),
            slot(MONDAY, 13, 0, 13, 30)
        ))).isTrue();
    }

    @Test
    void overlapAmong_sameTimeOnDifferentDatesDoesNotOverlap() {
        assertThat(TeacherLessonConflictChecker.overlapAmong(List.of(
            slot(MONDAY, 13, 0, 13, 30),
            slot(MONDAY.plusWeeks(1), 13, 0, 13, 30)
        ))).isFalse();
    }

    @Test
    void overlapAmong_emptyIsFalse() {
        assertThat(TeacherLessonConflictChecker.overlapAmong(List.of())).isFalse();
    }
}
