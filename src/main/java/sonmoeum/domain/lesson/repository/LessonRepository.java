package sonmoeum.domain.lesson.repository;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import sonmoeum.domain.lesson.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @EntityGraph(attributePaths = {"teacher", "subject"})
    List<Lesson> findAllByIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = {"teacher", "subject"})
    List<Lesson> findAllByTeacherIdAndIsDeletedFalseAndDateBetweenOrderByDateAscPeriodAsc(
        Long teacherId, LocalDate startDate, LocalDate endDate
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

    List<Lesson> findAllBySubjectIdAndDateGreaterThanEqualAndIsDeletedFalse(Long subjectId, LocalDate from);

    boolean existsByTeacherIdAndDateAndIsDeletedFalseAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
        Long teacherId, LocalDate date, LocalTime endTime, LocalTime startTime);

    boolean existsByTeacherIdAndDateAndIsDeletedFalseAndIdNotAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
        Long teacherId,
        LocalDate date,
        Long lessonId,
        LocalTime endTime,
        LocalTime startTime
    );
}

