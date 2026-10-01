package geumjeongyahak.domain.daily_schedule.service;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toMap;

import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.student.service.StudentProxyService;
import geumjeongyahak.domain.users.entity.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * (분반, 날짜)의 활성 수업에 맞춰 DailySchedule과 교사·학생 출석을 만들거나 고친다.
 * 수업이 하나도 없으면 DailySchedule과 출석을 지운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyScheduleSynchronizer {

    private final DailyScheduleRepository dailyScheduleRepository;
    private final DailyTeacherAttendanceRepository dailyTeacherAttendanceRepository;
    private final DailyStudentAttendanceRepository dailyStudentAttendanceRepository;
    private final LessonProxyService lessonProxyService;
    private final StudentProxyService studentProxyService;

    @Transactional
    public void synchronize(Long classroomId, LocalDate lessonDate) {
        List<Lesson> lessons = lessonProxyService.getActiveLessonsByClassroomAndDate(classroomId, lessonDate);
        if (lessons.isEmpty()) {
            deleteOrphan(classroomId, lessonDate);
            log.debug("DailySchedule 동기화 스킵 - 활성 수업 없음 (classroomId={}, lessonDate={})", classroomId, lessonDate);
            return;
        }

        Lesson representative = lessons.get(0);
        User teacher = representative.getTeacher();
        LocalTime activityStartTime = lessons.stream().map(Lesson::getStartTime).min(Comparator.naturalOrder()).orElseThrow();
        LocalTime activityEndTime = lessons.stream().map(Lesson::getEndTime).max(Comparator.naturalOrder()).orElseThrow();

        DailySchedule dailySchedule = dailyScheduleRepository.findByClassroomIdAndLessonDate(classroomId, lessonDate)
            .map(existing -> {
                existing.restore();
                return existing;
            })
            .orElseGet(() -> dailyScheduleRepository.save(new DailySchedule(
                representative.getSubject().getClassroom(), teacher, lessonDate, activityStartTime, activityEndTime
            )));

        dailySchedule.updateTeacher(teacher);
        dailySchedule.updateActivityTime(activityStartTime, activityEndTime);
        syncTeacherAttendance(dailySchedule, DailyTeacherAttendance.volunteerMinutesBetween(activityStartTime, activityEndTime));
        syncStudentAttendances(dailySchedule, classroomId);
    }

    private void syncTeacherAttendance(DailySchedule dailySchedule, Integer volunteerServiceMinutes) {
        DailyTeacherAttendance attendance = dailyTeacherAttendanceRepository.findByDailyScheduleId(dailySchedule.getId())
            .orElseGet(() -> dailyTeacherAttendanceRepository.save(
                new DailyTeacherAttendance(dailySchedule, volunteerServiceMinutes)
            ));
        attendance.restore();
        attendance.updateVolunteerServiceMinutes(volunteerServiceMinutes);
    }

    /** 일정의 출석을 한 번에 읽어, 있는 학생은 되살리고 없는 학생만 새로 만든다. */
    private void syncStudentAttendances(DailySchedule dailySchedule, Long classroomId) {
        Map<Long, DailyStudentAttendance> attendanceByStudentId = dailyStudentAttendanceRepository
            .findAllByDailyScheduleId(dailySchedule.getId())
            .stream()
            .collect(toMap(attendance -> attendance.getStudent().getId(), Function.identity(), (first, second) -> first));

        Map<Boolean, List<Student>> studentsByHasAttendance = studentProxyService
            .getActiveStudentsByClassroomId(classroomId)
            .stream()
            .collect(partitioningBy(student -> attendanceByStudentId.containsKey(student.getId())));

        studentsByHasAttendance.get(true)
            .forEach(student -> attendanceByStudentId.get(student.getId()).restore());

        List<DailyStudentAttendance> missing = studentsByHasAttendance.get(false).stream()
            .map(student -> new DailyStudentAttendance(dailySchedule, student))
            .toList();
        if (!missing.isEmpty()) {
            dailyStudentAttendanceRepository.saveAll(missing);
        }
    }

    private void deleteOrphan(Long classroomId, LocalDate lessonDate) {
        dailyScheduleRepository.findByClassroomIdAndLessonDateAndIsDeletedFalse(classroomId, lessonDate)
            .ifPresent(dailySchedule -> {
                dailyTeacherAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId())
                    .forEach(DailyTeacherAttendance::softDelete);
                dailyStudentAttendanceRepository.findAllByDailyScheduleIdAndIsDeletedFalse(dailySchedule.getId())
                    .forEach(DailyStudentAttendance::softDelete);
                dailySchedule.softDelete();
            });
    }
}
