package geumjeongyahak.domain.daily_schedule.repository;

import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyTeacherAttendanceRepository extends JpaRepository<DailyTeacherAttendance, Long> {

    Optional<DailyTeacherAttendance> findByDailyScheduleId(Long dailyScheduleId);

    Optional<DailyTeacherAttendance> findByDailyScheduleIdAndIsDeletedFalse(Long dailyScheduleId);

    List<DailyTeacherAttendance> findAllByDailyScheduleIdAndIsDeletedFalse(Long dailyScheduleId);
}
