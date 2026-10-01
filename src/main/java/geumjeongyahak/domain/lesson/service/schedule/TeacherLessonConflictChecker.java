package geumjeongyahak.domain.lesson.service.schedule;

import static geumjeongyahak.domain.lesson.repository.specification.LessonSpecs.excludingLesson;
import static geumjeongyahak.domain.lesson.repository.specification.LessonSpecs.excludingSubject;
import static geumjeongyahak.domain.lesson.repository.specification.LessonSpecs.isActive;
import static geumjeongyahak.domain.lesson.repository.specification.LessonSpecs.onDates;
import static geumjeongyahak.domain.lesson.repository.specification.LessonSpecs.overlapsTime;
import static geumjeongyahak.domain.lesson.repository.specification.LessonSpecs.taughtBy;

import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.repository.specification.LessonSpecs;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/** 교사 수업 시간 겹침 판정의 유일한 입구. 여러 날짜를 질의 한 번으로 본다. */
@Component
@RequiredArgsConstructor
public class TeacherLessonConflictChecker {

    private final LessonRepository lessonRepository;

    public Set<LocalDate> findConflictDates(
        Long teacherId,
        Collection<LocalDate> dates,
        LocalTime startTime,
        LocalTime endTime,
        ConflictExclusion exclusion
    ) {
        if (dates.isEmpty()) {
            return Set.of();
        }

        Specification<Lesson> spec = Specification.allOf(
            isActive(),
            taughtBy(teacherId),
            onDates(dates),
            overlapsTime(startTime, endTime)
        );
        if (exclusion.lessonId() != null) {
            spec = spec.and(excludingLesson(exclusion.lessonId()));
        }
        if (exclusion.subjectId() != null) {
            spec = spec.and(excludingSubject(exclusion.subjectId()));
        }

        return lessonRepository.findAll(spec).stream()
            .map(Lesson::getDate)
            .collect(Collectors.toSet());
    }

    public boolean hasConflict(
        Long teacherId,
        Collection<LocalDate> dates,
        LocalTime startTime,
        LocalTime endTime,
        ConflictExclusion exclusion
    ) {
        return !findConflictDates(teacherId, dates, startTime, endTime, exclusion).isEmpty();
    }

    /** 바꿀 수업들끼리 같은 날 시간이 겹치는지. 겹침 규칙은 {@link LessonSpecs#overlapsTime}과 같은 반열린 구간이다. */
    public static boolean overlapAmong(Collection<TimeSlot> slots) {
        return slots.stream()
            .collect(Collectors.groupingBy(TimeSlot::date))
            .values()
            .stream()
            .map(sameDate -> sameDate.stream().sorted(Comparator.comparing(TimeSlot::startTime)).toList())
            .anyMatch(sorted -> IntStream.range(1, sorted.size())
                .anyMatch(i -> sorted.get(i - 1).overlaps(sorted.get(i))));
    }

    /** 한 날짜의 수업 시간 [startTime, endTime). */
    public record TimeSlot(LocalDate date, LocalTime startTime, LocalTime endTime) {

        boolean overlaps(TimeSlot other) {
            return date.equals(other.date)
                && startTime.isBefore(other.endTime)
                && endTime.isAfter(other.startTime);
        }
    }

    /** 겹침 검사에서 뺄 수업. 자기 자신을 다시 저장할 때 자기와 겹치지 않게 한다. */
    public record ConflictExclusion(Long lessonId, Long subjectId) {

        public static final ConflictExclusion NONE = new ConflictExclusion(null, null);

        public static ConflictExclusion ofLesson(Long lessonId) {
            return new ConflictExclusion(lessonId, null);
        }

        public static ConflictExclusion ofSubject(Long subjectId) {
            return new ConflictExclusion(null, subjectId);
        }
    }
}
