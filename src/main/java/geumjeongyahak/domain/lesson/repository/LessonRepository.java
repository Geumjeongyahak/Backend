package geumjeongyahak.domain.lesson.repository;

import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.enums.LessonStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    long countByIsDeletedFalse();

    long countByIsDeletedFalseAndDate(LocalDate date);

    long countByStatusAndIsDeletedFalseAndDateBetween(LessonStatus status, LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = {"teacher", "subject"})
    List<Lesson> findAllByIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = {"teacher", "subject"})
    List<Lesson> findAllByIsDeletedFalseOrderByDateAscPeriodAsc();

    @EntityGraph(attributePaths = {"teacher", "subject"})
    List<Lesson> findAllByTeacherIdAndIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(
        Long teacherId, LocalDate startDate, LocalDate endDate
    );

    @EntityGraph(attributePaths = {"teacher", "subject", "subject.classroom"})
    List<Lesson> findAllByTeacher_IdAndDateAndIsDeletedFalse(Long teacherId, LocalDate date);

    @EntityGraph(attributePaths = {"teacher", "subject", "subject.classroom"})
    List<Lesson> findAllBySubjectClassroomIdAndDateAndIsDeletedFalseOrderByPeriodAscStartTimeAsc(
        Long classroomId,
        LocalDate date
    );

    @EntityGraph(attributePaths = {"teacher", "subject", "subject.classroom"})
    @Query("""
        SELECT l FROM Lesson l
        WHERE l.isDeleted = false
            AND l.subject.classroom.id IN :classroomIds
            AND l.date IN :dates
        ORDER BY l.date DESC, l.period ASC, l.startTime ASC
        """)
    List<Lesson> findAllActiveByClassroomIdsAndDates(
        @Param("classroomIds") List<Long> classroomIds,
        @Param("dates") List<LocalDate> dates
    );

    @EntityGraph(attributePaths = {"teacher", "subject", "subject.classroom"})
    List<Lesson> findAllByTeacher_IdAndDateAndPeriodBetweenAndIsDeletedFalse(
        Long teacherId,
        LocalDate date,
        Integer startPeriod,
        Integer endPeriod
    );

    @EntityGraph(attributePaths = {"teacher", "subject"})
    Optional<Lesson> findByIdAndIsDeletedFalse(Long lessonId);

    @EntityGraph(attributePaths = {"teacher", "subject"})
    Optional<Lesson> findByIdAndTeacherIdAndIsDeletedFalse(Long lessonId, Long teacherId);

    @EntityGraph(attributePaths = {"teacher", "subject"})
    Optional<Lesson> findByTeacherIdAndDateAndStartTimeAndEndTimeAndIsDeletedFalse(
        Long teacherId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    );

    @EntityGraph(attributePaths = {"teacher", "subject"})
    List<Lesson> findAllBySubjectIdAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(
        Long subjectId,
        LocalDate date
    );

    @EntityGraph(attributePaths = {"teacher", "subject"})
    List<Lesson> findAllBySubjectIdAndStatusAndIsDeletedFalseAndDateGreaterThanEqualOrderByDateAscPeriodAsc(
        Long subjectId,
        LessonStatus status,
        LocalDate startDate
    );

    boolean existsByTeacherIdAndDateAndIsDeletedFalseAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
        Long teacherId, LocalDate date, LocalTime endTime, LocalTime startTime);

    boolean existsByTeacherIdAndDateAndIsDeletedFalseAndIdNotAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
        Long teacherId,
        LocalDate date,
        Long lessonId,
        LocalTime endTime,
        LocalTime startTime
    );

    boolean existsByTeacherIdAndDateAndIsDeletedFalseAndSubjectIdNotAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
        Long teacherId,
        LocalDate date,
        Long subjectId,
        LocalTime endTime,
        LocalTime startTime
    );
}
