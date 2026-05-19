package geumjeongyahak.domain.daily_schedule.repository;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyScheduleRepository extends JpaRepository<DailySchedule, Long> {

    Optional<DailySchedule> findByClassroomIdAndLessonDateAndIsDeletedFalse(Long classroomId, LocalDate lessonDate);

    Optional<DailySchedule> findByClassroomIdAndLessonDate(Long classroomId, LocalDate lessonDate);

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    Optional<DailySchedule> findByTeacherIdAndLessonDateAndIsDeletedFalse(Long teacherId, LocalDate lessonDate);

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    List<DailySchedule> findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(
        LocalDate from,
        LocalDate to
    );

    @EntityGraph(attributePaths = {"classroom", "teacher"})
    Optional<DailySchedule> findByIdAndIsDeletedFalse(Long dailyScheduleId);
}
