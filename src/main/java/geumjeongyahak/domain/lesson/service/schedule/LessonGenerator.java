package geumjeongyahak.domain.lesson.service.schedule;

import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker.ConflictExclusion;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.users.entity.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 과목의 기간·요일로 수업을 만든다. 교사 시간이 겹치는 날짜는 건너뛴다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonGenerator {

    private final LessonRepository lessonRepository;
    private final TeacherLessonConflictChecker conflictChecker;

    public List<Lesson> generate(
        Subject subject,
        User teacher,
        LocalDate startAt,
        LocalDate endAt,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int period
    ) {
        List<LocalDate> dates = lessonDates(startAt, endAt, dayOfWeek);
        Set<LocalDate> conflictDates = conflictChecker.findConflictDates(
            teacher.getId(), dates, startTime, endTime, ConflictExclusion.NONE
        );
        if (!conflictDates.isEmpty()) {
            log.warn(
                "수업 자동 생성 스킵 - 교사 시간 충돌 (subjectId={}, teacherId={}, dates={})",
                subject.getId(),
                teacher.getId(),
                conflictDates.stream().sorted().toList()
            );
        }

        List<Lesson> lessons = dates.stream()
            .filter(date -> !conflictDates.contains(date))
            .map(date -> new Lesson(subject, teacher, date, startTime, endTime, period))
            .toList();
        return lessonRepository.saveAll(lessons);
    }

    /** startAt~endAt(양 끝 포함) 중 dayOfWeek인 날짜. startAt이 endAt보다 늦으면 비어 있다. */
    public static List<LocalDate> lessonDates(LocalDate startAt, LocalDate endAt, DayOfWeek dayOfWeek) {
        if (startAt.isAfter(endAt)) {
            return List.of();
        }
        return startAt.datesUntil(endAt.plusDays(1))
            .filter(date -> date.getDayOfWeek() == dayOfWeek)
            .toList();
    }
}
