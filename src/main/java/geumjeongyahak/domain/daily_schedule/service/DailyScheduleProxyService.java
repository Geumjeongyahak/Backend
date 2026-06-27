package geumjeongyahak.domain.daily_schedule.service;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.exception.DailyScheduleNotFoundException;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyScheduleProxyService {

    private final DailyScheduleRepository dailyScheduleRepository;
    private final DailyTeacherAttendanceRepository dailyTeacherAttendanceRepository;

    public DailySchedule getActiveById(Long dailyScheduleId) {
        return dailyScheduleRepository.findByIdAndIsDeletedFalse(dailyScheduleId)
            .orElseThrow(() -> new DailyScheduleNotFoundException(dailyScheduleId));
    }

    public DailySchedule getActiveByTeacherIdAndLessonDate(Long teacherId, LocalDate lessonDate) {
        return dailyScheduleRepository.findByTeacherIdAndLessonDateAndIsDeletedFalse(teacherId, lessonDate)
            .orElseThrow(() -> new DailyScheduleNotFoundException(
                "teacherId=" + teacherId + ", lessonDate=" + lessonDate
            ));
    }

    public Long findActiveIdByClassroomIdAndLessonDate(Long classroomId, LocalDate lessonDate) {
        return dailyScheduleRepository.findByClassroomIdAndLessonDateAndIsDeletedFalse(classroomId, lessonDate)
            .map(DailySchedule::getId)
            .orElse(null);
    }

    public DailySchedule findActiveByClassroomIdAndLessonDate(Long classroomId, LocalDate lessonDate) {
        return dailyScheduleRepository.findByClassroomIdAndLessonDateAndIsDeletedFalse(classroomId, lessonDate)
            .orElse(null);
    }

    public List<DailySchedule> findAllActiveBetween(LocalDate from, LocalDate to) {
        return dailyScheduleRepository
            .findAllByIsDeletedFalseAndLessonDateBetweenOrderByLessonDateAscIdAsc(from, to);
    }

    public Optional<DailyTeacherAttendance> findActiveTeacherAttendanceByDailyScheduleId(Long dailyScheduleId) {
        return dailyTeacherAttendanceRepository.findByDailyScheduleIdAndIsDeletedFalse(dailyScheduleId);
    }

    public List<DailyTeacherAttendance> findActiveTeacherAttendancesByDailyScheduleIds(
        Collection<Long> dailyScheduleIds
    ) {
        if (dailyScheduleIds.isEmpty()) {
            return List.of();
        }
        return dailyTeacherAttendanceRepository.findAllByDailyScheduleIdInAndIsDeletedFalse(dailyScheduleIds);
    }
}
