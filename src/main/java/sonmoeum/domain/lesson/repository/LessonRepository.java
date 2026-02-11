package sonmoeum.domain.lesson.repository;

import java.time.LocalDate;
import java.util.List;
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
}

