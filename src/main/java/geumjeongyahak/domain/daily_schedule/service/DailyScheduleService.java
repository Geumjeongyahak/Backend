package geumjeongyahak.domain.daily_schedule.service;

import geumjeongyahak.domain.classroom.entity.Classroom;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyScheduleService {

    private final DailyScheduleRepository dailyScheduleRepository;
    private final DailyTeacherAttendanceRepository dailyTeacherAttendanceRepository;
    private final DailyStudentAttendanceRepository dailyStudentAttendanceRepository;
    private final LessonProxyService lessonProxyService;
    private final StudentProxyService studentProxyService;

    @Transactional
    public void synchronizeByLesson(Lesson lesson) {
        Long classroomId = lesson.getSubject().getClassroom().getId();
        synchronizeByClassroomAndDate(classroomId, lesson.getDate());
    }

    @Transactional
    public void synchronizeByClassroomAndDate(Long classroomId, LocalDate lessonDate) {
        List<Lesson> lessons = lessonProxyService.getActiveLessonsByClassroomAndDate(classroomId, lessonDate);
        if (lessons.isEmpty()) {
            deleteOrphanDailySchedule(classroomId, lessonDate);
            log.debug("DailySchedule 동기화 스킵 - 활성 수업 없음 (classroomId={}, lessonDate={})", classroomId, lessonDate);
            return;
        }

        Lesson representativeLesson = lessons.get(0);
        Classroom classroom = representativeLesson.getSubject().getClassroom();
        User teacher = representativeLesson.getTeacher();
        LocalTime activityStartTime = lessons.stream()
            .map(Lesson::getStartTime)
            .min(LocalTime::compareTo)
            .orElse(null);
        LocalTime activityEndTime = lessons.stream()
            .map(Lesson::getEndTime)
            .max(LocalTime::compareTo)
            .orElse(null);
        Integer volunteerServiceMinutes = calculateVolunteerServiceMinutes(activityStartTime, activityEndTime);

        DailySchedule dailySchedule = dailyScheduleRepository
            .findByClassroomIdAndLessonDate(classroomId, lessonDate)
            .map(existing -> {
                existing.restore();
                return existing;
            })
            .orElseGet(() -> dailyScheduleRepository.save(new DailySchedule(
                classroom,
                teacher,
                lessonDate,
                activityStartTime,
                activityEndTime
            )));

        dailySchedule.updateTeacher(teacher);
        dailySchedule.updateActivityTime(activityStartTime, activityEndTime);
        initializeTeacherAttendance(dailySchedule, volunteerServiceMinutes);
        initializeStudentAttendances(dailySchedule, classroomId);
    }

    private Integer calculateVolunteerServiceMinutes(LocalTime activityStartTime, LocalTime activityEndTime) {
        if (activityStartTime == null || activityEndTime == null) {
            return null;
        }
        return Math.toIntExact(Duration.between(activityStartTime, activityEndTime).toMinutes());
    }

    private void initializeTeacherAttendance(DailySchedule dailySchedule, Integer volunteerServiceMinutes) {
        DailyTeacherAttendance attendance = dailyTeacherAttendanceRepository
            .findByDailyScheduleId(dailySchedule.getId())
            .orElseGet(() -> dailyTeacherAttendanceRepository.save(new DailyTeacherAttendance(
                dailySchedule,
                volunteerServiceMinutes
            )));
        attendance.restore();
        attendance.updateVolunteerServiceMinutes(volunteerServiceMinutes);
    }

    private void initializeStudentAttendances(DailySchedule dailySchedule, Long classroomId) {
        List<Student> activeStudents = studentProxyService.getActiveStudentsByClassroomId(classroomId);
        for (Student student : activeStudents) {
            DailyStudentAttendance attendance = dailyStudentAttendanceRepository
                .findByDailyScheduleIdAndStudentId(dailySchedule.getId(), student.getId())
                .orElseGet(() -> dailyStudentAttendanceRepository.save(new DailyStudentAttendance(dailySchedule, student)));
            attendance.restore();
        }
    }

    private void deleteOrphanDailySchedule(Long classroomId, LocalDate lessonDate) {
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
