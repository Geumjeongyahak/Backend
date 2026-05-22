package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.auth.enums.RoleType;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.daily_schedule.entity.DailySchedule;
import geumjeongyahak.domain.daily_schedule.entity.DailyStudentAttendance;
import geumjeongyahak.domain.daily_schedule.entity.DailyTeacherAttendance;
import geumjeongyahak.domain.daily_schedule.repository.DailyScheduleRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyStudentAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.repository.DailyTeacherAttendanceRepository;
import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.lesson.entity.Lesson;
import geumjeongyahak.domain.lesson.service.LessonProxyService;
import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.student.service.StudentProxyService;
import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.users.entity.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DailyScheduleServiceTest {

    @Mock
    private DailyScheduleRepository dailyScheduleRepository;

    @Mock
    private DailyTeacherAttendanceRepository dailyTeacherAttendanceRepository;

    @Mock
    private DailyStudentAttendanceRepository dailyStudentAttendanceRepository;

    @Mock
    private LessonProxyService lessonProxyService;

    @Mock
    private StudentProxyService studentProxyService;

    @InjectMocks
    private DailyScheduleService dailyScheduleService;

    @Test
    void synchronizeByClassroomAndDate_createsScheduleAndInitialAttendances() {
        LocalDate lessonDate = LocalDate.of(2026, 5, 20);
        Classroom classroom = classroom(1L);
        User teacher = teacher("홍길동");
        Subject subject = subject(classroom, teacher, lessonDate);
        Lesson firstLesson = lesson(subject, teacher, lessonDate, LocalTime.of(14, 0), LocalTime.of(15, 0), 1);
        Lesson secondLesson = lesson(subject, teacher, lessonDate, LocalTime.of(15, 10), LocalTime.of(16, 0), 2);
        Student student = student(10L, classroom);

        given(lessonProxyService.getActiveLessonsByClassroomAndDate(
            classroom.getId(),
            lessonDate
        )).willReturn(List.of(firstLesson, secondLesson));
        given(dailyScheduleRepository.findByClassroomIdAndLessonDate(classroom.getId(), lessonDate))
            .willReturn(Optional.empty());
        given(dailyScheduleRepository.save(any(DailySchedule.class))).willAnswer(invocation -> {
            DailySchedule dailySchedule = invocation.getArgument(0);
            ReflectionTestUtils.setField(dailySchedule, "id", 100L);
            return dailySchedule;
        });
        given(dailyTeacherAttendanceRepository.findByDailyScheduleId(100L)).willReturn(Optional.empty());
        given(dailyTeacherAttendanceRepository.save(any(DailyTeacherAttendance.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(studentProxyService.getActiveStudentsByClassroomId(classroom.getId()))
            .willReturn(List.of(student));
        given(dailyStudentAttendanceRepository.findByDailyScheduleIdAndStudentId(100L, student.getId()))
            .willReturn(Optional.empty());
        given(dailyStudentAttendanceRepository.save(any(DailyStudentAttendance.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        dailyScheduleService.synchronizeByClassroomAndDate(classroom.getId(), lessonDate);

        ArgumentCaptor<DailySchedule> dailyScheduleCaptor = ArgumentCaptor.forClass(DailySchedule.class);
        verify(dailyScheduleRepository).save(dailyScheduleCaptor.capture());
        DailySchedule dailySchedule = dailyScheduleCaptor.getValue();
        assertThat(dailySchedule.getClassroom()).isEqualTo(classroom);
        assertThat(dailySchedule.getTeacher()).isEqualTo(teacher);
        assertThat(dailySchedule.getActivityStartTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(dailySchedule.getActivityEndTime()).isEqualTo(LocalTime.of(16, 0));

        ArgumentCaptor<DailyTeacherAttendance> teacherAttendanceCaptor =
            ArgumentCaptor.forClass(DailyTeacherAttendance.class);
        verify(dailyTeacherAttendanceRepository).save(teacherAttendanceCaptor.capture());
        assertThat(teacherAttendanceCaptor.getValue().getVolunteerServiceMinutes()).isEqualTo(120);

        ArgumentCaptor<DailyStudentAttendance> studentAttendanceCaptor =
            ArgumentCaptor.forClass(DailyStudentAttendance.class);
        verify(dailyStudentAttendanceRepository).save(studentAttendanceCaptor.capture());
        assertThat(studentAttendanceCaptor.getValue().getStudent()).isEqualTo(student);
    }

    private Classroom classroom(Long id) {
        Classroom classroom = Classroom.builder()
            .name("장미반")
            .type(ClassroomType.WEEKDAY)
            .build();
        ReflectionTestUtils.setField(classroom, "id", id);
        return classroom;
    }

    private User teacher(String name) {
        return User.builder()
            .name(name)
            .role(RoleType.VOLUNTEER)
            .build();
    }

    private Subject subject(Classroom classroom, User teacher, LocalDate lessonDate) {
        return new Subject(
            classroom,
            teacher,
            "국어",
            lessonDate,
            lessonDate.plusMonths(1),
            DayOfWeek.WEDNESDAY,
            LocalTime.of(14, 0),
            LocalTime.of(16, 0),
            1,
            LocalDateTime.now(),
            null
        );
    }

    private Lesson lesson(
        Subject subject,
        User teacher,
        LocalDate lessonDate,
        LocalTime startTime,
        LocalTime endTime,
        int period
    ) {
        return new Lesson(subject, teacher, lessonDate, startTime, endTime, period);
    }

    private Student student(Long id, Classroom classroom) {
        Student student = new Student("최양지", null, null, classroom);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }
}
