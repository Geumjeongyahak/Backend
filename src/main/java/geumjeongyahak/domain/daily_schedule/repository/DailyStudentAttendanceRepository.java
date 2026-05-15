package geumjeongyahak.domain.daily_schedule.repository;

import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyStudentAttendanceRepository extends JpaRepository<DailyStudentAttendance, Long> {

    List<DailyStudentAttendance> findAllByDailyScheduleIdAndIsDeletedFalse(Long dailyScheduleId);

    Optional<DailyStudentAttendance> findByDailyScheduleIdAndStudentId(Long dailyScheduleId, Long studentId);
}
