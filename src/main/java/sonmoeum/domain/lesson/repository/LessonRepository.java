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
    List<Lesson> findAllByDateBetweenOrderByDateAscPeriodAsc(LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = {"teacher", "subject"})
    List<Lesson> findAllByTeacherIdAndDateBetweenOrderByDateAscPeriodAsc(
        Long teacherId, LocalDate startDate, LocalDate endDate
    );

    @EntityGraph(attributePaths = {"teacher", "subject"})
    Optional<Lesson> findById(Long lessonId);

    @EntityGraph(attributePaths = {"teacher", "subject"})
    Optional<Lesson> findByIdAndTeacherId(Long lessonId, Long teacherId);

    boolean existsByTeacherIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
        Long id, LocalDate date, LocalTime startTime, LocalTime endTime);
}

