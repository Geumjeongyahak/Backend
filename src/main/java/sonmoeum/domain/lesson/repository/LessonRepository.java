package sonmoeum.domain.lesson.repository;

import java.time.LocalDate;
import java.util.List;

import sonmoeum.domain.lesson.entity.Lesson;
import sonmoeum.domain.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findAllByTeacherAndDate(User teacher, LocalDate date);
    List<Lesson> findAllByTeacherAndDateBetween(User teacher, LocalDate startDate, LocalDate endDate);
}

