package sonmoeum.domain.lesson.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.users.entity.User;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @EntityGraph(attributePaths = {"teacher", "subject"})
    List<Lesson> findAllByDateBetweenOrderByDateAscPeriodAsc(LocalDate startDate, LocalDate endDate);
    List<Lesson> findAllByTeacherAndDate(User teacher, LocalDate date);
    List<Lesson> findAllByTeacherAndDateBetween(User teacher, LocalDate startDate, LocalDate endDate);
}

