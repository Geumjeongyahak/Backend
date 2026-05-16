package geumjeongyahak.domain.lesson.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.lesson.dto.LessonTeacherDate;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import geumjeongyahak.domain.lesson.exception.LessonNotFoundException;
import geumjeongyahak.domain.lesson.repository.LessonRepository;

/**
 * Lesson 도메인의 Proxy Service.
 * 다른 도메인(request 등)에서 Lesson 엔티티에 접근할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
public class LessonProxyService {

    private final LessonRepository lessonRepository;

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

    @Transactional
    public void updateActiveLessonsStatusByClassroomAndDate(Long classroomId, LocalDate date, LessonStatus status) {
        lessonRepository.findAllBySubjectClassroomIdAndDateAndIsDeletedFalseOrderByPeriodAscStartTimeAsc(
            classroomId,
            date
        ).forEach(lesson -> lesson.updateStatus(status));
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
        return lessonRepository.existsByTeacherIdAndDateAndIsDeletedFalseAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            teacherId,
            date,
            endTime,
            startTime
        );
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

    @Transactional(readOnly = true)
    public boolean existsTeacherConflictForFutureSubjectScheduledLessons(
        Long subjectId,
        Long teacherId,
        LocalDate from
    ) {
        List<Lesson> lessons = lessonRepository
            .findAllBySubjectIdAndStatusAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(
                subjectId,
                LessonStatus.SCHEDULED,
                from
            );

        return lessons.stream()
            .anyMatch(lesson -> lessonRepository
                .existsByTeacherIdAndDateAndIsDeletedFalseAndIdNotAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                    teacherId,
                    lesson.getDate(),
                    lesson.getId(),
                    lesson.getEndTime(),
                    lesson.getStartTime()
                )
            );
    }

    @Transactional(readOnly = true)
    public boolean existsTeacherConflictForFutureSubjectScheduledLessons(
        Long subjectId,
        Long teacherId,
        LocalDate from,
        LocalTime startTime,
        LocalTime endTime
    ) {
        List<Lesson> lessons = lessonRepository
            .findAllBySubjectIdAndStatusAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(
                subjectId,
                LessonStatus.SCHEDULED,
                from
            );

        return lessons.stream()
            .anyMatch(lesson -> lessonRepository
                .existsByTeacherIdAndDateAndIsDeletedFalseAndIdNotAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                    teacherId,
                    lesson.getDate(),
                    lesson.getId(),
                    endTime,
                    startTime
                )
            );
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
        List<LocalDate> dates = startAt.datesUntil(endAt.plusDays(1))
            .filter(date -> date.getDayOfWeek() == dayOfWeek)
            .toList();

        return dates.stream()
            .anyMatch(date -> lessonRepository
                .existsByTeacherIdAndDateAndIsDeletedFalseAndSubjectIdNotAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                    teacherId,
                    date,
                    subjectId,
                    endTime,
                    startTime
                )
            );
    }
}
