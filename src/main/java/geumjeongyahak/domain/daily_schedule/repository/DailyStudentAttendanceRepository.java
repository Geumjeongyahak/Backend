package geumjeongyahak.domain.daily_schedule.repository;

import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyStudentAttendanceRepository extends JpaRepository<DailyStudentAttendance, Long> {

    @EntityGraph(attributePaths = {"student"})
    List<DailyStudentAttendance> findAllByDailyScheduleIdAndIsDeletedFalse(Long dailyScheduleId);

    @EntityGraph(attributePaths = {"dailySchedule", "student"})
    List<DailyStudentAttendance> findAllByDailySchedule_IdInAndIsDeletedFalse(List<Long> dailyScheduleIds);

    /** 삭제된 출석도 포함한다. 동기화할 때 되살리려고 읽는다. */
    List<DailyStudentAttendance> findAllByDailyScheduleId(Long dailyScheduleId);
}
