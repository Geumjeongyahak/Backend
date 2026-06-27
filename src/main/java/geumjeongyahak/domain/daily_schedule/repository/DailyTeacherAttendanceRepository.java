package geumjeongyahak.domain.daily_schedule.repository;

import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.enums.DailyScheduleStatus;
import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyTeacherAttendanceRepository extends JpaRepository<DailyTeacherAttendance, Long> {

    Optional<DailyTeacherAttendance> findByDailyScheduleId(Long dailyScheduleId);

    Optional<DailyTeacherAttendance> findByDailyScheduleIdAndIsDeletedFalse(Long dailyScheduleId);

    List<DailyTeacherAttendance> findAllByDailyScheduleIdAndIsDeletedFalse(Long dailyScheduleId);

    @Query("""
        select coalesce(sum(attendance.volunteerServiceMinutes), 0)
        from DailyTeacherAttendance attendance
        join attendance.dailySchedule dailySchedule
        where attendance.isDeleted = false
            and dailySchedule.isDeleted = false
            and dailySchedule.status = :dailyScheduleStatus
            and attendance.status in :includedAttendanceStatuses
            and dailySchedule.teacher.id = :teacherId
            and (:from is null or dailySchedule.lessonDate >= :from)
            and (:to is null or dailySchedule.lessonDate <= :to)
        """)
    Long sumVolunteerServiceMinutes(
        @Param("teacherId") Long teacherId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("dailyScheduleStatus") DailyScheduleStatus dailyScheduleStatus,
        @Param("includedAttendanceStatuses") Collection<DailyTeacherAttendanceStatus> includedAttendanceStatuses
    );
}
