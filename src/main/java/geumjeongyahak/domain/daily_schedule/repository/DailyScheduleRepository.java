package geumjeongyahak.domain.daily_schedule.repository;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyScheduleRepository extends JpaRepository<DailySchedule, Long> {

    Optional<DailySchedule> findByClassroomIdAndLessonDate(Long classroomId, LocalDate lessonDate);

    boolean existsByClassroomIdAndLessonDate(Long classroomId, LocalDate lessonDate);
}
