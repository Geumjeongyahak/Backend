package geumjeongyahak.domain.lesson.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.lesson.dto.LessonTeacherDate;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.exception.LessonNotFoundException;
import geumjeongyahak.domain.lesson.repository.LessonRepository;
import geumjeongyahak.domain.lesson.service.schedule.LessonGenerator;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker.ConflictExclusion;
import geumjeongyahak.domain.lesson.service.schedule.TeacherLessonConflictChecker.TimeSlot;
import geumjeongyahak.domain.users.entity.User;

/**
 * Lesson 도메인의 Proxy Service.
 * 다른 도메인(request 등)에서 Lesson 엔티티에 접근할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
public class LessonProxyService {

    private final LessonRepository lessonRepository;
    private final TeacherLessonConflictChecker conflictChecker;

    /**
     * 삭제되지 않은 수업 조회. 없으면 예외 발생.
     */
    @Transactional(readOnly = true)
    public Lesson getActiveById(Long lessonId) {
        return lessonRepository.findByIdAndIsDeletedFalse(lessonId)
            .orElseThrow(() -> new LessonNotFoundException(lessonId));
    }

    @Transactional(readOnly = true)
    public List<Lesson> getActiveLessonsByTeacherAndDate(Long teacherId, LocalDate date) {
        return lessonRepository.findAllByTeacher_IdAndDateAndIsDeletedFalse(teacherId, date);
    }

    @Transactional(readOnly = true)
    public List<Lesson> getActiveLessonsByClassroomAndDate(Long classroomId, LocalDate date) {
        return lessonRepository.findAllBySubjectClassroomIdAndDateAndIsDeletedFalseOrderByPeriodAscStartTimeAsc(
            classroomId,
            date
        );
    }

    @Transactional(readOnly = true)
    public List<Lesson> getActiveLessonsByClassroomIdsAndDates(Set<Long> classroomIds, Set<LocalDate> dates) {
        if (classroomIds.isEmpty() || dates.isEmpty()) {
            return List.of();
        }
        return lessonRepository.findAllActiveByClassroomIdsAndDates(
            List.copyOf(classroomIds),
            List.copyOf(dates)
        );
    }

    @Transactional
    public void updateActiveLessonsStatusByClassroomAndDate(Long classroomId, LocalDate date, LessonStatus status) {
        lessonRepository.findAllBySubjectClassroomIdAndDateAndIsDeletedFalseOrderByPeriodAscStartTimeAsc(
            classroomId,
            date
        ).forEach(lesson -> lesson.updateStatus(status));
    }

    @Transactional
    public void updateActiveLessonsTeacherByClassroomAndDate(Long classroomId, LocalDate date, User teacher) {
        lessonRepository.findAllBySubjectClassroomIdAndDateAndIsDeletedFalseOrderByPeriodAscStartTimeAsc(
            classroomId,
            date
        ).forEach(lesson -> lesson.updateTeacher(teacher));
    }

    @Transactional(readOnly = true)
    public List<Lesson> getActiveLessonsByTeacherAndDateAndPeriodBetween(
        Long teacherId,
        LocalDate date,
        Integer startPeriod,
        Integer endPeriod
    ) {
        return lessonRepository.findAllByTeacher_IdAndDateAndPeriodBetweenAndIsDeletedFalse(
            teacherId,
            date,
            startPeriod,
            endPeriod
        );
    }

    @Transactional(readOnly = true)
    public boolean existsActiveLessonConflict(
        Long teacherId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    ) {
        return conflictChecker.hasConflict(teacherId, List.of(date), startTime, endTime, ConflictExclusion.NONE);
    }

    @Transactional(readOnly = true)
    public boolean existsFutureActiveLessonBySubjectId(Long subjectId, LocalDate from) {
        return !lessonRepository
            .findAllBySubjectIdAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(subjectId, from)
            .isEmpty();
    }

    @Transactional(readOnly = true)
    public List<Long> getFutureActiveLessonIdsBySubjectId(Long subjectId, LocalDate from) {
        return lessonRepository
            .findAllBySubjectIdAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(subjectId, from)
            .stream()
            .map(Lesson::getId)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LessonTeacherDate> getFutureActiveLessonTeacherDatesBySubjectId(Long subjectId, LocalDate from) {
        return lessonRepository
            .findAllBySubjectIdAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(subjectId, from)
            .stream()
            .map(lesson -> new LessonTeacherDate(lesson.getTeacher().getId(), lesson.getDate()))
            .distinct()
            .toList();
    }

    @Transactional(readOnly = true)
    public boolean existsUnchangeableFutureActiveLessonBySubjectId(Long subjectId, LocalDate from) {
        return lessonRepository
            .findAllBySubjectIdAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(subjectId, from)
            .stream()
            .anyMatch(lesson ->
                lesson.getStatus() != LessonStatus.SCHEDULED
                    || (lesson.getNote() != null && !lesson.getNote().isBlank())
            );
    }

    /**
     * 과목의 미래 예정 수업들을 teacherId가 맡았을 때 겹침이 생기는지 본다.
     * startTime·endTime이 null이면 각 수업의 지금 시간으로, 아니면 새 시간으로 본다.
     * 교사의 다른 과목 수업과의 겹침, 그리고 바꾼 뒤 같은 날 이 과목 수업끼리의 겹침을 둘 다 본다.
     */
    @Transactional(readOnly = true)
    public boolean existsTeacherConflictForFutureSubjectScheduledLessons(
        Long subjectId,
        Long teacherId,
        LocalDate from,
        LocalTime startTime,
        LocalTime endTime
    ) {
        List<TimeSlot> planned = lessonRepository
            .findAllBySubjectIdAndStatusAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(
                subjectId,
                LessonStatus.SCHEDULED,
                from
            )
            .stream()
            .map(lesson -> new TimeSlot(
                lesson.getDate(),
                startTime != null ? startTime : lesson.getStartTime(),
                endTime != null ? endTime : lesson.getEndTime()
            ))
            .toList();

        if (TeacherLessonConflictChecker.overlapAmong(planned)) {
            return true;
        }
        return planned.stream()
            .collect(Collectors.groupingBy(
                slot -> List.of(slot.startTime(), slot.endTime()),
                Collectors.mapping(TimeSlot::date, Collectors.toList())
            ))
            .entrySet()
            .stream()
            .anyMatch(entry -> conflictChecker.hasConflict(
                teacherId,
                entry.getValue(),
                entry.getKey().get(0),
                entry.getKey().get(1),
                ConflictExclusion.ofSubject(subjectId)
            ));
    }

    @Transactional(readOnly = true)
    public boolean existsTeacherConflictForSubjectSchedule(
        Long subjectId,
        Long teacherId,
        LocalDate startAt,
        LocalDate endAt,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
    ) {
        return conflictChecker.hasConflict(
            teacherId,
            LessonGenerator.lessonDates(startAt, endAt, dayOfWeek),
            startTime,
            endTime,
            ConflictExclusion.ofSubject(subjectId)
        );
    }
}
