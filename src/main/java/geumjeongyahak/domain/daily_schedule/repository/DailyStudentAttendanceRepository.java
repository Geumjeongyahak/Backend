package geumjeongyahak.domain.daily_schedule.repository;

import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyStudentAttendanceRepository extends JpaRepository<DailyStudentAttendance, Long> {

    @EntityGraph(attributePaths = {"student"})
    List<DailyStudentAttendance> findAllByDailyScheduleIdAndIsDeletedFalse(Long dailyScheduleId);

    List<DailyStudentAttendance> findAllByDailySchedule_IdInAndIsDeletedFalse(List<Long> dailyScheduleIds);

    Optional<DailyStudentAttendance> findByDailyScheduleIdAndStudentId(Long dailyScheduleId, Long studentId);
}
